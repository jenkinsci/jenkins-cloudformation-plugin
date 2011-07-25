/**
 * 
 */
package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * @author erickdovale
 * 
 */
public class CloudFormationBuildWrapper extends BuildWrapper {

	protected List<StackBean> stacks;
	
	public CloudFormationBuildWrapper(List<StackBean> stackBeans) {
		this.stacks = stackBeans;
	}

	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {
		
		final List<CloudFormation> cloudFormations = new ArrayList<CloudFormation>();
		final Map<String, String> stackOutputs =  new HashMap<String, String>();
		
		for (StackBean stackBean : stacks) {

			final CloudFormation cloudFormation = newCloudFormation(stackBean, build, listener.getLogger());
			
			if (cloudFormation.create()){
				cloudFormations.add(cloudFormation);
				stackOutputs.putAll(cloudFormation.getOutputs());
			} else{
				build.setResult(Result.FAILURE);
				return null;
			}

		}


		return new Environment() {
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener)
					throws IOException, InterruptedException {
				
				boolean result = true;
				
				for (CloudFormation cf : cloudFormations) {
					result = result && cf.delete();
				}
				
				return result;

			}

			@Override
			public void buildEnvVars(Map<String, String> env) {
				env.putAll(stackOutputs);
			}
		};
	}

	protected CloudFormation newCloudFormation(StackBean stackBean, AbstractBuild build,
			PrintStream logger) throws IOException {
		return new CloudFormation(logger, stackBean);
	}

	private String cloudFormationRecipe(StackBean stackBean, AbstractBuild build) throws IOException {
		return build.getWorkspace().child(stackBean.getCloudFormationRecipe()).readToString();
	}
	
	@Extension
	public static class DescriptorImpl extends BuildWrapperDescriptor {
		
		public DescriptorImpl() {
			super(CloudFormationBuildWrapper.class);
		}

        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            List<StackBean> stacks = req.bindParametersToList(StackBean.class, "stack.stack.");
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
