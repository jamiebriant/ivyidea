package org.clarent.ivyidea.intellij.facet;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.clarent.ivyidea.intellij.facet.ui.IvyFacetEditorTab;

/**
 * @author Guy Mahieu
 */

public class IvyFacetConfiguration implements FacetConfiguration {

    public FacetEditorTab[] createEditorTabs(FacetEditorContext editorContext, FacetValidatorsManager validatorsManager) {
        return new FacetEditorTab[] { new IvyFacetEditorTab() };
    }

    public void readExternal(Element element) throws InvalidDataException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeExternal(Element element) throws WriteExternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
