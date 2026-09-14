package com.OneDesK.helpers;

public class ValidationUtils {


	public static boolean isValidEmail(String email) {
		boolean resultado=true;
		
		if(!email.contains("@")) {
			resultado= false;
			}
		return resultado;
	}
	public static boolean tieneMasDe(String texto,int cant) {
		boolean resultado=true;
		
		if(texto.length()<cant) {
			resultado= false;
			}
		return resultado;
	}
	
}
