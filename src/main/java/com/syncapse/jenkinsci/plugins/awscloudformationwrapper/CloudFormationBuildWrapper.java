/**
 * 
 */
package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * @author erickdovale
 * 
 */
public class CloudFormationBuildWrapper extends BuildWrapper {

    private static final Logger LOGGER = Logger.getLogger(CloudFormationBuildWrapper.class.getName());

    protected List<StackBean> stacks;

	public CloudFormationBuildWrapper(List<StackBean> stackBeans) {
		this.stacks = stackBeans;
	}

	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {

		final List<CloudFormation> cloudFormations = new ArrayList<CloudFormation>();

        EnvVars env = build.getEnvironment(listener);
        env.overrideAll(build.getBuildVariables());

		for (StackBean stackBean : stacks) {

			final CloudFormation cloudFormation = newCloudFormation(stackBean,
					build, env, listener.getLogger());

			if (cloudFormation.create()) {
				cloudFormations.add(cloudFormation);
				env.putAll(cloudFormation.getOutputs());
			} else {
				build.setResult(Result.FAILURE);
			}

		}

		return new Environment() {
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener)
					throws IOException, InterruptedException {

				boolean result = true;

				List<CloudFormation> reverseOrder = new ArrayList<CloudFormation>(cloudFormations);
				Collections.reverse(reverseOrder);

				for (CloudFormation cf : reverseOrder) {
					result = result && cf.delete();
				}

				return result;

			}

		};
	}

	protected CloudFormation newCloudFormation(StackBean stackBean,
			AbstractBuild<?, ?> build, EnvVars env, PrintStream logger) throws IOException {
		
		return new CloudFormation(logger, stackBean.getStackName(), build
				.getWorkspace().child(stackBean.getCloudFormationRecipe())
				.readToString(), stackBean.getParsedParameters(env),
				stackBean.getTimeout(), stackBean.getAwsAccessKey(),
				stackBean.getAwsSecretKey());
		
	}

	@Extension
	public static class DescriptorImpl extends BuildWrapperDescriptor {

		public DescriptorImpl() {
			super(CloudFormationBuildWrapper.class);
		}

		@Override
		public BuildWrapper newInstance(StaplerRequest req,
				final JSONObject formData) throws FormException {
			List<StackBean> stacks = req.bindParametersToList(StackBean.class,
					"stack.stack.");
			return new CloudFormationBuildWrapper(stacks);
		}

		@Override
		public String getDisplayName() {
			return "Create AWS Cloud Formation stack";
		}

		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return true;
		}

	}

	public List<StackBean> getStacks() {
		return stacks;
	}

}
