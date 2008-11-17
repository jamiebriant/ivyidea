package org.clarent.ivyidea.resolve;

import com.intellij.openapi.module.Module;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.DefaultRepositoryCacheManager;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.settings.IvySettings;
import org.clarent.ivyidea.config.IvyIdeaConfigHelper;
import org.clarent.ivyidea.config.exception.IvySettingsNotFoundException;
import org.clarent.ivyidea.intellij.IntellijUtils;
import org.clarent.ivyidea.ivy.IvyManager;
import org.clarent.ivyidea.ivy.IvyUtil;
import org.clarent.ivyidea.resolve.dependency.*;
import org.clarent.ivyidea.resolve.problem.ResolveProblem;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Guy Mahieu
 */

class DependencyResolver {

    private static final Logger LOGGER = Logger.getLogger(DependencyResolver.class.getName());

    private final List<ResolveProblem> problems;

    public DependencyResolver() {
        problems = new ArrayList<ResolveProblem>();
    }

    public List<ResolveProblem> getProblems() {
        return Collections.unmodifiableList(problems);
    }

    public List<ResolvedDependency> resolve(Module module, IvyManager ivyManager) throws IvySettingsNotFoundException {
        final Ivy ivy = ivyManager.getIvy(module);
        final File ivyFile = IvyUtil.getIvyFile(module);
        try {
            final ResolveReport resolveReport = ivy.resolve(ivyFile.toURI().toURL(), IvyIdeaConfigHelper.createResolveOptions(module));
            return extractDependencies(resolveReport, ivy.getSettings(), new ModuleDependencies(module, ivyManager));
        } catch (ParseException e) {
            throw new RuntimeException("The ivy file " + ivyFile.getAbsolutePath() + " could not be parsed correctly!", e);
        } catch (IOException e) {
            throw new RuntimeException("The ivy file " + ivyFile.getAbsolutePath() + " could not be accessed!", e);
        }
    }

    @NotNull
    protected List<ResolvedDependency> extractDependencies(ResolveReport resolveReport, IvySettings ivySettings, ModuleDependencies moduleDependencies) {
        List<ResolvedDependency> result = new ArrayList<ResolvedDependency>();
        List<IvyNode> dependencies = getDependencies(resolveReport);
        for (IvyNode dependency : dependencies) {
            final String resolverName = ivySettings.getResolverName(dependency.getResolvedId());
            final org.apache.ivy.plugins.resolver.DependencyResolver resolver = ivySettings.getResolver(resolverName);
            RepositoryCacheManager repositoryCacheManager = resolver.getRepositoryCacheManager();
            if (repositoryCacheManager instanceof DefaultRepositoryCacheManager) {
                DefaultRepositoryCacheManager defaultRepositoryCacheManager = (DefaultRepositoryCacheManager) repositoryCacheManager;
                final ModuleRevisionId dependencyRevisionId = dependency.getResolvedId();
                if (moduleDependencies.isModuleDependency(dependencyRevisionId.getModuleId())) {
                    result.add(new InternalDependency(moduleDependencies.getModuleDependency(dependencyRevisionId.getModuleId())));
                } else {
                    if (dependency.hasProblem()) {
                        //noinspection ThrowableResultOfMethodCallIgnored
                        problems.add(new ResolveProblem(
                                dependency.getId().toString(),
                                dependency.getProblemMessage(),
                                dependency.getProblem()));
                        LOGGER.info("DEPENDENCY PROBLEM: " + dependency.getId() + ": " + dependency.getProblemMessage());
                    } else {
                        if (dependency.isCompletelyEvicted()) {
                            LOGGER.info("Not adding evicted dependency " + dependency);
                        } else if (dependency.isCompletelyBlacklisted()) {
                            // From quickly looking at the ivy sources, i think this means that there was a conflict,
                            // and this dependency lost in the conflict resolution - don't know how this is different
                            // from evicted modules, but it is probably not something we want to add
                            LOGGER.info("Not adding blacklisted dependency " + dependency);
                        } else {
                            final Artifact[] artifacts = dependency.getAllArtifacts();
                            for (Artifact artifact : artifacts) {
                                final ExternalDependency externalDependency = createExternalDependency(defaultRepositoryCacheManager, artifact);
                                if (externalDependency != null) {
                                    if (externalDependency.isMissing()) {
                                        problems.add(new ResolveProblem(
                                                artifact.getModuleRevisionId().toString(),
                                                "file not found: " + externalDependency.getExternalArtifact().getAbsolutePath())
                                        );
                                    } else {
                                        result.add(externalDependency);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // TODO Check with Ivy dev team if the getArchiveFileInCache method shouldn't be in the interface
                // TODO         ===> Request logged: https://issues.apache.org/jira/browse/IVY-912
                throw new RuntimeException("Unsupported RepositoryCacheManager type: " + repositoryCacheManager.getClass().getName());
            }

        }
        return result;
    }

    @Nullable
    private ExternalDependency createExternalDependency(DefaultRepositoryCacheManager defaultRepositoryCacheManager, Artifact artifact) {
        final File file = defaultRepositoryCacheManager.getArchiveFileInCache(artifact);
        ResolvedArtifact resolvedArtifact = new ResolvedArtifact(artifact);
        if (resolvedArtifact.isSourceType()) {
            return new ExternalSourceDependency(file);
        }
        if (resolvedArtifact.isJavaDocType()) {
            return new ExternalJavaDocDependency(file);
        }
        if (resolvedArtifact.isClassesType()) {
            return new ExternalJarDependency(file);
        }
        problems.add(new ResolveProblem(
                artifact.getModuleRevisionId().toString(),
                "Unrecognized artifact type: " + artifact.getType() + ", will not add this as a dependency in IntelliJ.",
                null));
        LOGGER.warning("Artifact of unrecognized type " + artifact.getType() + " found, *not* adding as a dependency.");
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<IvyNode> getDependencies(ResolveReport resolveReport) {
        return resolveReport.getDependencies();
    }

    /**
     * Holds the link between IntelliJ {@link com.intellij.openapi.module.Module}s and ivy
     * {@link org.apache.ivy.core.module.id.ModuleRevisionId}s
     */
    private static class ModuleDependencies {

        private IvyManager ivyManager;

        private Module module;

        private Map<ModuleId, Module> moduleDependencies = new HashMap<ModuleId, Module>();

        public ModuleDependencies(Module module, IvyManager ivyManager) throws IvySettingsNotFoundException {
            this.module = module;
            this.ivyManager = ivyManager;
            fillModuleDependencies();
        }

        public boolean isModuleDependency(ModuleId moduleId) {
            return moduleDependencies.containsKey(moduleId);
        }

        public Module getModuleDependency(ModuleId moduleId) {
            return moduleDependencies.get(moduleId);
        }

        private void fillModuleDependencies() throws IvySettingsNotFoundException {
            final File ivyFile = IvyUtil.getIvyFile(module);
            final ModuleDescriptor descriptor = IvyUtil.parseIvyFile(ivyFile, ivyManager.getIvy(module).getSettings());
            if (descriptor != null) {
                final DependencyDescriptor[] ivyDependencies = descriptor.getDependencies();
                for (Module dependencyModule : IntellijUtils.getAllModulesWithIvyIdeaFacet()) {
                    if (!module.equals(dependencyModule)) {
                        for (DependencyDescriptor ivyDependency : ivyDependencies) {
                            final ModuleId ivyDependencyId = ivyDependency.getDependencyId();
                            final ModuleId dependencyModuleId = getModuleId(dependencyModule);
                            if (ivyDependencyId.equals(dependencyModuleId)) {
                                LOGGER.info("Recognized dependency " + ivyDependency + " as intellij module '" + dependencyModule.getName() + "' in this project!");
                                moduleDependencies.put(dependencyModuleId, dependencyModule);
                                break;
                            }
                        }
                    }
                }
            }
        }

        @Nullable
        private ModuleId getModuleId(Module module) throws IvySettingsNotFoundException {
            final IvySettings ivySettings = ivyManager.getIvy(module).getSettings();
            if (!moduleDependencies.values().contains(module)) {
                final ModuleDescriptor ivyModuleDescriptor = IvyUtil.getIvyModuleDescriptor(module, ivySettings);
                if (ivyModuleDescriptor != null) {
                    moduleDependencies.put(ivyModuleDescriptor.getModuleRevisionId().getModuleId(), module);
                }

            }
            for (ModuleId moduleId : moduleDependencies.keySet()) {
                if (module.equals(moduleDependencies.get(moduleId))) {
                    return moduleId;
                }
            }
            return null;
        }


    }
}
