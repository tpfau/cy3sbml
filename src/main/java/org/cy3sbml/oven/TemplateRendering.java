package org.cy3sbml.oven;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;


/**
 * Template rendering with velocity.
 */
public class TemplateRendering {

	public static void test(){
		/*  first, get and initialize an engine  */
	    VelocityEngine ve = new VelocityEngine();
	    ve.init();
	    /*  next, get the Template  */
	    Template t = ve.getTemplate( "helloworld.vm" );
	    /*  create a context and add data */
	    VelocityContext context = new VelocityContext();
	    context.put("name", "World");
	    /* now render the template into a StringWriter */
	    StringWriter writer = new StringWriter();
	    t.merge( context, writer );
	    /* show the World */
	    System.out.println( writer.toString() );	
	}

	public static void main(String[] args){
	    test();
    }


}
