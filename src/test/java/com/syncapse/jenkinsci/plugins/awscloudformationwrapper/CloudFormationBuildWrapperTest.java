package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CloudFormationBuildWrapperTest {
	
	private CloudFormationBuildWrapper wrapper; // SUT
	
	@Test
	public void parameterParsing_1_Param() {
		
		String parameters = "key1=value1";
		wrapper = new CloudFormationBuildWrapper("name", "description", "aRecipe", parameters, "0", "awsAccessKey", "awsSecretKey");
		
		assertTrue(wrapper.parsedParameters.get("key1").equals("value1"));
		assertTrue(wrapper.parsedParameters.values().size() == 1);
		
	}

	@Test
	public void parameterParsing_2_Params() {
		
		String parameters = "key1=value1, key2=value2"; // make sure spaces don't mess things up.
		wrapper = new CloudFormationBuildWrapper("name", "description", "aRecipe", parameters, "0", "awsAccessKey", "awsSecretKey");
		
		assertTrue(wrapper.parsedParameters.get("key1").equals("value1"));
		assertTrue(wrapper.parsedParameters.get("key2").equals("value2"));
		assertTrue(wrapper.parsedParameters.values().size() == 2);
		
	}

	@Test
	public void parameterParsing_Is_Resilient_to_spaces() {
		
		String parameters = "key1 = value1,     key2    =     value2"; // make sure spaces don't mess things up.
		wrapper = new CloudFormationBuildWrapper("name", "description", "aRecipe", parameters, "0", "awsAccessKey", "awsSecretKey");
		
		assertTrue(wrapper.parsedParameters.get("key1").equals("value1"));
		assertTrue(wrapper.parsedParameters.get("key2").equals("value2"));
		assertTrue(wrapper.parsedParameters.values().size() == 2);
		
	}

}
