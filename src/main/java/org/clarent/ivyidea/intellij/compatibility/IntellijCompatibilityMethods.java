/*
 * Copyright 2010 Guy Mahieu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.clarent.ivyidea.intellij.compatibility;

import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.OrderRootType;

/**
 * Interface defining methods that are impacted by API differences in supported
 * versions of Intellij IDEA
 *
 * @author Guy Mahieu
 */
public interface IntellijCompatibilityMethods {

    OrderRootType[] getAllOrderRootTypes();

    OrderRootType getJavadocOrderRootType();

    ModuleType getJavaModuleType();

    boolean isTaskCancelledOnProgressIndicatorCancel();

}
