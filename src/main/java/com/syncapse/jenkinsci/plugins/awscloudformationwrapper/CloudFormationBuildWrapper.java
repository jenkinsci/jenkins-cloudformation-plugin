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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author erickdovale
 * 
 */
public class CloudFormationBuildWrapper extends BuildWrapper {

	protected List<StackBean> stacks;

	private transient Map<Integer, CloudFormation> cloudFormationsMap = new HashMap<Integer, CloudFormation>();

	@DataBoundConstructor
	public CloudFormationBuildWrapper(List<StackBean> stacks) {
		this.stacks = stacks;
	}

	@Override
	public void makeBuildVariables(AbstractBuild build,
			Map<String, String> variables) {
			if(build != null && cloudFormationsMap.get(build.number) != null){
			variables.putAll(cloudFormationsMap.get(build.number).getOutputs());
		}

	}

	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {
        EnvVars env = build.getEnvironment(listener);
        env.overrideAll(build.getBuildVariables());
        boolean success = true;

		for (StackBean stackBean : stacks) {

			final CloudFormation cloudFormation = newCloudFormation(stackBean,
					build, env, listener.getLogger());

			try {
				if (cloudFormation.create()) {
					//This is dirty fix for now as if we have more than one stackbean, it'll be overwritten
					cloudFormationsMap.put(build.number, cloudFormation);
					env.putAll(cloudFormation.getOutputs());
				} else {
					build.setResult(Result.FAILURE);
					success = false;
					break;
				}
			} catch (TimeoutException e) {
				listener.getLogger()
						.append("ERROR creating stack with name "
								+ stackBean.getStackName()
								+ ". Operation timedout. Try increasing the timeout period in your stack configuration.");
				build.setResult(Result.FAILURE);
				success = false;
				break;
			}

		}
		
		// If any stack fails to create then destroy them all
		if (!success) {
			doTearDown(build);
			return null;
		}

		return new Environment() {
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener)
					throws IOException, InterruptedException {

				return doTearDown(build);
				
			}

		};
	}
	
	protected boolean doTearDown(AbstractBuild build) throws IOException, InterruptedException{
		boolean result = true;
		CloudFormation cf = cloudFormationsMap.get(build.number);
		if(cf.getAutoDeleteStack()) {
			result = result && cf.delete();
		}
		return result;
	}

	protected CloudFormation newCloudFormation(StackBean stackBean,
			AbstractBuild<?, ?> build, EnvVars env, PrintStream logger)
			throws IOException {

		Boolean isURL = false;
		String recipe = null;
		
		if(CloudFormation.isRecipeURL(stackBean.getCloudFormationRecipe())) {
			isURL = true;
			recipe = stackBean.getCloudFormationRecipe();
		} else {
			recipe = build.getWorkspace().child(stackBean.getCloudFormationRecipe()).readToString();
		}

		return new CloudFormation(logger, stackBean.getStackName(), isURL,
				recipe, stackBean.getParsedParameters(env),
				stackBean.getTimeout(), stackBean.getParsedAwsAccessKey(env),
				stackBean.getParsedAwsSecretKey(env),
				stackBean.getAwsRegion(), stackBean.getAutoDeleteStack(), env,false);

	}

	@Extension
	public static class DescriptorImpl extends BuildWrapperDescriptor {

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

	/**
	 * @return
	 */
	private Object readResolve() {
		// Initialize the cloud formation collection during deserialization to avoid NPEs. 
		cloudFormationsMap = new HashMap<Integer, CloudFormation>();
		return this;
	}
	
}
