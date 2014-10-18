/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.batch

import groovy.transform.CompileStatic
import com.netflix.spinnaker.orca.Task
import com.netflix.spinnaker.orca.pipeline.Stage
import com.netflix.spinnaker.orca.spring.AutowiredComponentBuilder
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.job.builder.JobFlowBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.beans.factory.annotation.Autowired
import static com.netflix.spinnaker.orca.batch.TaskTaskletAdapter.decorate

/**
 * Base class for a component that builds a _stage_ to be run as (part of) a
 * _pipeline_ and is backed by an underlying Spring Batch model.
 *
 * Note a _stage_ does not directly correspond to anything in Batch – perhaps a
 * {@link org.springframework.batch.core.job.flow.Flow}
 */
@CompileStatic
abstract class StageBuilder implements AutowiredComponentBuilder {

  final String type

  private StepBuilderFactory steps

  StageBuilder(String type) {
    this.type = type
  }

  /**
   * Implementations should construct any steps necessary for the stage and append them to +jobBuilder+. This method
   * is typically called when the stage is not the first in the pipeline.
   *
   * @param jobBuilder the builder for the job. Implementations should append steps to this.
   * @param stage the stage configuration.
   * @return the resulting builder after any steps are appended.
   */
  abstract JobFlowBuilder build(JobFlowBuilder jobBuilder, Stage stage)

  /**
   * Builds a Spring Batch +Step+ from an Orca +Task+ using required naming
   * convention.
   *
   * @param taskName The simple name for the task within the context of the stage.
   * @param taskType The +Task+ implementation class.
   * @return a +Step+ that will execute an instance of the required +Task+.
   */
  protected Step buildStep(String taskName, Class<? extends Task> taskType) {
    steps.get(stepName(taskName))
         .listener(PipelineStatusPropagationListener.instance)
         .tasklet(buildTask(taskType))
         .build()
  }

  /**
   * Builds a Spring Batch +Step+ from an Orca +Task+ using required naming
   * convention.
   *
   * @param taskName The simple name for the task within the context of the stage.
   * @param task The +Task+ implementation.
   * @return a +Step+ that will execute the specified +Task+.
   */
  protected Step buildStep(String taskName, Task task) {
    steps.get(stepName(taskName))
         .listener(PipelineStatusPropagationListener.instance)
         .tasklet(decorate(task))
         .build()
  }

  /**
   * Builds and autowires a task.
   *
   * @param taskType The +Task+ implementation class.
   * @return a +Tasklet+ that wraps the task implementation. This can be appended to the job as a tasklet step.
   * @see org.springframework.batch.core.step.builder.StepBuilder#tasklet(org.springframework.batch.core.step.tasklet.Tasklet)
   */
  private Tasklet buildTask(Class<? extends Task> taskType) {
    def task = taskType.newInstance()
    autowire task
    decorate task
  }

  private String stepName(String taskName) {
    "${type}.${taskName}"
  }

  @Autowired
  void setSteps(StepBuilderFactory steps) {
    this.steps = steps
  }
}