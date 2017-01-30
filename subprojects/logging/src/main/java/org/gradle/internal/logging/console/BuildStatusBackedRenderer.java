/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.logging.console;

import org.gradle.internal.logging.events.OperationIdentifier;
import org.gradle.internal.logging.events.OutputEvent;
import org.gradle.internal.logging.events.OutputEventListener;
import org.gradle.internal.logging.events.ProgressCompleteEvent;
import org.gradle.internal.logging.events.ProgressEvent;
import org.gradle.internal.logging.events.ProgressStartEvent;
import org.gradle.internal.logging.events.RenderNowEvent;
import org.gradle.internal.logging.text.Span;
import org.gradle.internal.logging.text.Style;
import org.gradle.internal.nativeintegration.console.ConsoleMetaData;

import java.util.Arrays;

public class BuildStatusBackedRenderer implements OutputEventListener {
    private final OutputEventListener listener;
    private final Label buildStatusLabel;
    private final ConsoleMetaData consoleMetaData;
    private String currentBuildStatus;
    private OperationIdentifier rootOperationId;

    public BuildStatusBackedRenderer(OutputEventListener listener, Label buildStatusLabel, ConsoleMetaData consoleMetaData) {
        this.listener = listener;
        this.buildStatusLabel = buildStatusLabel;
        this.consoleMetaData = consoleMetaData;
    }

    private void buildStarted(ProgressStartEvent progressStartEvent) {
        currentBuildStatus = progressStartEvent.getShortDescription();
    }

    private void buildProgressed(ProgressEvent progressEvent) {
        currentBuildStatus = progressEvent.getStatus();
    }

    private void buildFinished(ProgressCompleteEvent progressCompleteEvent) {
        currentBuildStatus = "";
    }

    @Override
    public void onOutput(OutputEvent event) {
        if (event instanceof ProgressStartEvent) {
            ProgressStartEvent startEvent = (ProgressStartEvent) event;
            // if it has no parent ID, assign this operation as the root operation
            if (startEvent.getParentId() == null && "org.gradle.internal.progress.BuildProgressLogger".equals(startEvent.getCategory())) {
                rootOperationId = startEvent.getOperationId();
                buildStarted(startEvent);
            }
        } else if (event instanceof ProgressCompleteEvent) {
            ProgressCompleteEvent completeEvent = (ProgressCompleteEvent) event;
            if (completeEvent.getOperationId().equals(rootOperationId)) {
                rootOperationId = null;
                buildFinished(completeEvent);
            }
        } else if (event instanceof ProgressEvent) {
            ProgressEvent progressEvent = (ProgressEvent) event;
            if (progressEvent.getOperationId().equals(rootOperationId)) {
                buildProgressed(progressEvent);
            }
        } else if (event instanceof RenderNowEvent) {
            renderNow();
        }
        listener.onOutput(event);
    }

    private String trimToConsole(String str) {
        int width = consoleMetaData.getCols() - 1;
        if (width > 0 && width < str.length()) {
            return str.substring(0, width);
        }
        return str;
    }

    private void renderNow() {
        if (currentBuildStatus != null) {
            buildStatusLabel.setText(Arrays.asList(new Span(Style.of(Style.Emphasis.BOLD), trimToConsole(currentBuildStatus))));
        }
    }
}
