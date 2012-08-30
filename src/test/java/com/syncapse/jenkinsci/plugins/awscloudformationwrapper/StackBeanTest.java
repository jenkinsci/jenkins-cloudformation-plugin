package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import hudson.EnvVars;

import org.junit.Before;
import org.junit.Test;

public class StackBeanTest {
	
	private StackBean stackBean; // SUT
	private EnvVars env;
	
	@Before
	public void setup() throws Exception {
		env = new EnvVars();
	}
	
	@Test
	public void parameterParsing_1_Param() {
		
		String parameters = "key1=value1";
		stackBean = new StackBean("name", "description", "aRecipe", parameters, 0, "awsAccessKey", "awsSecretKey", "region", true);
		
		assertTrue(stackBean.getParsedParameters(env).get("key1").equals("value1"));
		assertTrue(stackBean.getParsedParameters(env).values().size() == 1);
		
	}

	@Test
	public void parameterParsing_2_Params() {
		
		String parameters = "key1=value1, key2=value2"; // make sure spaces don't mess things up.
		stackBean = new StackBean("name", "description", "aRecipe", parameters, 0, "awsAccessKey", "awsSecretKey", "region", true);
		
		assertTrue(stackBean.getParsedParameters(env).get("key1").equals("value1"));
		assertTrue(stackBean.getParsedParameters(env).get("key2").equals("value2"));
		assertTrue(stackBean.getParsedParameters(env).values().size() == 2);
		
	}

	@Test
	public void parameterParsing_Is_Resilient_to_spaces() {
		
		String parameters = "key1 = value1,     key2    =     value2"; // make sure spaces don't mess things up.
		stackBean = new StackBean("name", "description", "aRecipe", parameters, 0, "awsAccessKey", "awsSecretKey", "region", true);
		
		assertTrue(stackBean.getParsedParameters(env).get("key1").equals("value1"));
		assertTrue(stackBean.getParsedParameters(env).get("key2").equals("value2"));
		assertTrue(stackBean.getParsedParameters(env).values().size() == 2);
		
	}
	
	@Test
	public void parsingParameters_Expand_Variables() throws Exception {
		env.put("value1", "expandedValue1");
		env.put("value2", "expandedValue2");
		String parameters = "key1=$value1,key2=${value2}, key3=$value3, key4=${value4}";
		stackBean = new StackBean("name", "description", "aRecipe", parameters, 0, "awsAccessKey", "awsSecretKey", "region", true);
		
		assertTrue(stackBean.getParsedParameters(env).get("key1").equals("expandedValue1"));
		assertTrue(stackBean.getParsedParameters(env).get("key2").equals("expandedValue2"));
		assertTrue(stackBean.getParsedParameters(env).get("key3").equals("$value3"));
		assertTrue(stackBean.getParsedParameters(env).get("key4").equals("${value4}"));
		assertTrue(stackBean.getParsedParameters(env).values().size() == 4);
		
	}
	
	@Test
	public void parsingParameters_With_Semicolons_Expand_Variables() throws Exception {
		env.put("value1", "expandedValue1");
		env.put("value2", "expandedValue2");
		String parameters = "key1=$value1;key2=${value2}; key3=v1,v2,v3,v4; key4=${value4}";
		stackBean = new StackBean("name", "description", "aRecipe", parameters, 0, "awsAccessKey", "awsSecretKey", "region", true);
		
		assertTrue(stackBean.getParsedParameters(env).get("key1").equals("expandedValue1"));
		assertTrue(stackBean.getParsedParameters(env).get("key2").equals("expandedValue2"));
		assertTrue(stackBean.getParsedParameters(env).get("key3").equals("v1,v2,v3,v4"));
		assertTrue(stackBean.getParsedParameters(env).get("key4").equals("${value4}"));
		assertTrue(stackBean.getParsedParameters(env).values().size() == 4);
		
	}

}
