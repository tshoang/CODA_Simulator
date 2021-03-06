/*******************************************************************************
 * (c) Crown owned copyright (2017) (UK Ministry of Defence)
 *
 * All rights reserved. This program and the accompanying materials are 
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      University of Southampton - Initial API and implementation
 *******************************************************************************/
package org.coda.simulator.animation.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StateResultStringParser {
	
	private String input;

	public StateResultStringParser(String input){
		this.input = input;
	}

	public Map<Integer, String> parse(){
		// if the string is an empty set then return null
		if(input.equals("\u2205")){
			return null;
		}
		Map<Integer, String> map = new HashMap<Integer, String>();
		
		String[] inputs = input.split(",");
		List<String> trimmedList = new ArrayList<String>();
		List<String> inputList = Arrays.asList(inputs);
		for(String s: inputList){
			s = s.replace('{', ' ');
			s = s.replace('}', ' ');
			s = s.replace('(', ' ');
			s= s.replace(')', ' ').trim();
			trimmedList.add(s);
		}
		
		for(String s: trimmedList){
			// split on maplet |->
			String[] split = s.split("\u21a6");
			
			Integer i = new Integer(split[0]);
			if(i != null){
				map.put(i,split[1]);
			}
		}
		return map;
	}
	
	public Map<Integer, String> parseReverse(){
		// if the string is an empty set then return null
		if(input.equals("\u2205")){
			return null;
		}
		Map<Integer, String> map = new HashMap<Integer, String>();
		
		String[] inputs = input.split(",");
		List<String> trimmedList = new ArrayList<String>();
		List<String> inputList = Arrays.asList(inputs);
		for(String s: inputList){
			s = s.replace('{', ' ');
			s = s.replace('}', ' ');
			s = s.replace('(', ' ');
			s= s.replace(')', ' ').trim();
			trimmedList.add(s);
		}
		
		for(String s: trimmedList){
			// split on maplet |->
			String[] split = s.split("\u21a6");
			
			Integer i = new Integer(split[1]);
			if(i != null){
				map.put(i,split[0]);
			}
		}
		return map;
	}
}
