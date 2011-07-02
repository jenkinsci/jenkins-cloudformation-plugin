package com.syncapse.jenkinsci.plugins;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.google.common.collect.Lists;

public class AWSCloudFormationBuilder extends Builder {
	
	/**
	 * The name of the stack.
	 */
	private final String stackName;

    /**
     * The description of the cloud formation stack that will be launched.
     */
    private final String description;
    
    /**
     * The json file with the Cloud Formation definition.
     */
    private final String cloudFormationRecipe;
    
	/**
     * The parameters to be passed into the cloud formation.
     */
    private final String parameters;
    
    /**
     * The access key to call Amazon's APIs 
     */
    private final String awsAccessKey;
    
    /**
     * The secret key to call Amazon's APIs
     */
    private final String awsSecretKey;
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public AWSCloudFormationBuilder(String stackName, String description, String cloudFormationRecipe, String parameters, String awsAccessKey, String awsSecretKey) {
        this.description = description;
        this.cloudFormationRecipe = cloudFormationRecipe;
        this.parameters = parameters;
        this.stackName = stackName;
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
    	
    	AWSCredentials credentials = new BasicAWSCredentials(this.awsAccessKey, this.awsSecretKey);
    	AmazonCloudFormation amazonClient = new AmazonCloudFormationAsyncClient(credentials);
    	
    	CreateStackRequest request;
		try {
			request = createStackRequest(build);
	    	CreateStackResult createStack = amazonClient.createStack(request);
	    	waitForStack(createStack.getStackId());
	        return true;
		} catch (IOException e) {
			build.setResult(Result.FAILURE);
			return false;
		}
        
    }

    private void waitForStack(String stackId) {
	}

	private CreateStackRequest createStackRequest(AbstractBuild<?,?> build) throws IOException {
    	
    	CreateStackRequest r = new CreateStackRequest();
    	r.withStackName(stackName);
    	r.withParameters(buildParammeters());
    	
    	r.withTemplateBody(build.getWorkspace().child(cloudFormationRecipe).readToString());
    	
		return r;
	}

	private List<Parameter> buildParammeters() {
		
		if (parameters == null || parameters.length() == 0){
			return null;
		}
		
		List<String> params = Lists.newArrayList(parameters.split(","));
		String[] tokens = null;
		List<Parameter> result = Lists.newArrayList();
		Parameter parameter = null;
		for (String param : params) {
			tokens = param.split("=");
			parameter = new Parameter();
			parameter.setParameterKey(tokens[0]);
			parameter.setParameterValue(tokens[1]);
			result.add(parameter);
		}
		
		return result;
	}

	// overrided for better type safety.
    // if your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link AWSCloudFormationBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>views/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private boolean useFrench;

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckName(@QueryParameter String value) throws IOException, ServletException {
            if(value.length()==0)
                return FormValidation.error("Please set a name");
            if(value.length()<4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Create cloud formation stack";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         */
        public boolean useFrench() {
            return useFrench;
        }
    }

    public String getDescription() {
		return description;
	}

	public String getCloudFormationRecipe() {
		return cloudFormationRecipe;
	}

	public String getParameters() {
		return parameters;
	}

	/**
	 * @return the stackName
	 */
	public String getStackName() {
		return stackName;
	}

	/**
	 * @return the awsAccessKey
	 */
	public String getAwsAccessKey() {
		return awsAccessKey;
	}

	/**
	 * @return the awsSecretKey
	 */
	public String getAwsSecretKey() {
		return awsSecretKey;
	}

}

