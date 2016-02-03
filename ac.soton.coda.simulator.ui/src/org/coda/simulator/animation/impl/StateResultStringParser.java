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
