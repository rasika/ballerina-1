/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ballerina.plugins.idea.configuration;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Responsible for providing Ballerina configurations in Settings window.
 */
public class BallerinaConfigurableProvider extends ConfigurableProvider {

    @NotNull
    private final Project myProject;
    private Configurable projectsSettingsConfigurable;

    public BallerinaConfigurableProvider(@NotNull Project project) {
        myProject = project;
    }

    @Nullable
    @Override
    public Configurable createConfigurable() {
        if (projectsSettingsConfigurable == null) {
            projectsSettingsConfigurable = new BallerinaProjectSettingsConfigurable(myProject);
        }
        return projectsSettingsConfigurable;
    }
}
