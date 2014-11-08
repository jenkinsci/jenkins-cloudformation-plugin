/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import static com.syncapse.jenkinsci.plugins.awscloudformationwrapper.CloudFormationPostBuildNotifier.DESCRIPTOR;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author amit.gilad
 */
public class CloudFormationPostBuildNotifier extends Notifier{
    	private static final Logger LOGGER = Logger.getLogger(CloudFormationPostBuildNotifier.class.getName());
	private final List<PostBuildStackBean> stacks;

	@DataBoundConstructor
	public CloudFormationPostBuildNotifier(List<PostBuildStackBean> stacks) {
		this.stacks = stacks;
	}

	public List<PostBuildStackBean> getStacks() {
		return stacks;
	}


	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		LOGGER.info("prebuild");
		return super.prebuild(build, listener);
	}

       
	@Override
	public Action getProjectAction(AbstractProject<?, ?> project) {
		LOGGER.info("getProjectAction");
		return super.getProjectAction(project);
	}

	@Override
	public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
		LOGGER.info("getProjectActions");
		return super.getProjectActions(project);
	}
  
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		EnvVars envVars = build.getEnvironment(listener);
                envVars.overrideAll(build.getBuildVariables());
		boolean result = true;
                  
                 
		for (PostBuildStackBean stack : stacks) {
		final CloudFormation cloudFormation = newCloudFormation(stack,build, envVars, listener.getLogger());	
                    /*CloudFormation cloudFormation = new CloudFormation(
					listener.getLogger(),
					stack.getStackName(),
					"",
					new HashMap<String, String>(),
					0,
					stack.getParsedAwsAccessKey(envVars),
					stack.getParsedAwsSecretKey(envVars),
					stack.getAwsRegion(),
					false,
					envVars
			);*/
			if(cloudFormation.create()) {
				LOGGER.info("Success");
			} else {
				LOGGER.warning("Failed");
				result = false;
			}
		}
		return result;
	}
	protected CloudFormation newCloudFormation(PostBuildStackBean postBuildStackBean,
			AbstractBuild<?, ?> build, EnvVars env, PrintStream logger)
			throws IOException {

		return new CloudFormation(logger, postBuildStackBean.getStackName(), build
				.getWorkspace().child(postBuildStackBean.getCloudFormationRecipe())
				.readToString(), postBuildStackBean.getParsedParameters(env),
				postBuildStackBean.getTimeout(), postBuildStackBean.getParsedAwsAccessKey(env),
				postBuildStackBean.getParsedAwsSecretKey(env),
				postBuildStackBean.getAwsRegion(), env,false,postBuildStackBean.getSleep());

	}
	@Override
	public BuildStepDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Extension
	public static final CloudFormationPostBuildNotifier.DescriptorImpl DESCRIPTOR = new CloudFormationPostBuildNotifier.DescriptorImpl();

	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		@Override
		public String getDisplayName() {
                    
			return "AWS Cloud Formation";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
                
	}
}
