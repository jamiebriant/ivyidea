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

package org.clarent.ivyidea.intellij.task;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.Task;

/**
 * @author Guy Mahieu
 */

public abstract class IvyIdeaBackgroundTask extends Task.Backgroundable {

    private static final PerformInBackgroundOption BackgroundOptionStartWithDialog = new PerformInBackgroundOption() {
        public boolean shouldStartInBackground() {
            return false;
        }

        public void processSentToBackground() {
        }

        public void processRestoredToForeground() {
        }
    };

    public IvyIdeaBackgroundTask(AnActionEvent event) {
        super(DataKeys.PROJECT.getData(event.getDataContext()),
                "IvyIDEA " + event.getPresentation().getText(),
                true, BackgroundOptionStartWithDialog);
    }
}
