/*
 * Copyright 2015-2018 Norbert Potocki (norbert.potocki@nort.pl)
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
package org.cfg4j.source.compose;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.assertj.core.data.MapEntry;
import org.cfg4j.source.ConfigurationSource;
import org.cfg4j.source.context.environment.Environment;
import org.cfg4j.source.context.environment.ImmutableEnvironment;
import org.cfg4j.source.context.environment.MissingEnvironmentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.Properties;


class MergeConfigurationSourceTest {




  private ConfigurationSource[] underlyingSources;
  private MergeConfigurationSource mergeConfigurationSource;

  @BeforeEach
  void setUp() {
    underlyingSources = new ConfigurationSource[5];
    for (int i = 0; i < underlyingSources.length; i++) {
      underlyingSources[i] = mock(ConfigurationSource.class);
      when(underlyingSources[i].getConfiguration(any(Environment.class))).thenReturn(new Properties());
    }

    mergeConfigurationSource = new MergeConfigurationSource(underlyingSources);
    mergeConfigurationSource.init();
  }

  @Test
  void getConfigurationThrowsWhenOneOfSourcesThrowsOnMissingEnvironment() {
    when(underlyingSources[1].getConfiguration(ArgumentMatchers.any())).thenThrow(new MissingEnvironmentException(""));

    assertThatThrownBy(() -> mergeConfigurationSource.getConfiguration(new ImmutableEnvironment("test"))).isExactlyInstanceOf(MissingEnvironmentException.class);
  }

  @Test
  void getConfigurationThrowsWhenOneOfSourcesThrows() {
    when(underlyingSources[3].getConfiguration(ArgumentMatchers.any())).thenThrow(new IllegalStateException());

    assertThatThrownBy(() -> mergeConfigurationSource.getConfiguration(new ImmutableEnvironment("test"))).isExactlyInstanceOf(IllegalStateException.class);
  }

  @Test
  void getConfigurationMergesConfigurations() {
    Environment environment = new ImmutableEnvironment("test");

    sourcesWithProps(environment, "prop1", "value1", "prop2", "value2");

    assertThat(mergeConfigurationSource.getConfiguration(environment)).containsOnly(MapEntry.entry("prop1", "value1"),
        MapEntry.entry("prop2", "value2"));
  }

  @Test
  void getConfigurationMergesConfigurationsWithCollidingKeys() {
    Environment environment = new ImmutableEnvironment("test");

    sourcesWithProps(environment, "prop", "value1", "prop", "value2");

    assertThat(mergeConfigurationSource.getConfiguration(environment)).containsOnly(MapEntry.entry("prop", "value2"));
  }

  @Test
  void initInitializesAllSources() {
    for (ConfigurationSource underlyingSource : underlyingSources) {
      verify(underlyingSource, atLeastOnce()).init();
    }
  }

  private void sourcesWithProps(Environment environment, String... props) {
    Properties[] properties = getProps(props);

    for (int i = 0; i < properties.length; i++) {
      when(underlyingSources[i].getConfiguration(environment)).thenReturn(properties[i]);
    }
  }

  private Properties[] getProps(String... props) {
    Properties[] properties = new Properties[props.length / 2];
    for (int i = 1; i < props.length; i += 2) {
      properties[i / 2] = new Properties();
      properties[i / 2].put(props[i - 1], props[i]);
    }

    return properties;
  }
}