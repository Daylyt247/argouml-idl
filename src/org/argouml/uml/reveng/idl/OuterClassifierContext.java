/* $Id$
 *****************************************************************************
 * Copyright (c) 2009-2012 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    tfmorris
 *****************************************************************************
 *
 * Some portions of this file was previously release using the BSD License:
 */

// Copyright (c) 2003-2007 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies.  This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason.  IN NO EVENT SHALL THE
// UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

package org.argouml.uml.reveng.idl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.argouml.model.Model;
import org.argouml.uml.reveng.ImportClassLoader;

/**
 * This context is an outer class containing inner classes.
 *
 * @author Marcus Andersson
 */
class OuterClassifierContext extends Context {
    
    private static final Logger LOG = 
        Logger.getLogger(OuterClassifierContext.class.getName());
    
    /** The classifier this context represents. */
    private Object mClassifier;

    /** The package this classifier belongs to */
    private Object mPackage;

    /** This is appended to classname when searching in classpath. */
    private String namePrefix;

    /** The java style name of the package. */
    private String packageJavaName;

    /**
     Create a new context from a classifier.

     @param base Based on this context.
     @param theClassifier The classifier.
     @param thePackage The package the classifier belongs to.
     @param theNamePrefix Inner class prefix, like "OuterClassname$"
    */
    public OuterClassifierContext(Context base,
				  Object theClassifier,
				  Object thePackage,
				  String theNamePrefix) {
	super(base);
	this.mClassifier = theClassifier;
	this.mPackage = thePackage;
	this.namePrefix = theNamePrefix;
	packageJavaName = getJavaName(thePackage);
    }

    public Object getInterface(String name)
	throws ClassifierNotFoundException {
        return get(name, true);
    }

    /**
     * Get a classifier from the model. If it is not in the model, try
     * to find it with the CLASSPATH. If found, in the classpath, the
     * classifier is created and added to the model. If not found at
     * all, a datatype is created and added to the model.
     *
     * @param name The name of the classifier to find.
     * @return Found classifier.
     */
    public Object get(String name)
        throws ClassifierNotFoundException {
        return get(name, false);
    }

    public Object get(String name, boolean interfacesOnly)
	throws ClassifierNotFoundException {
	// Search in classifier
	Object iClassifier = Model.getFacade().lookupIn(mClassifier, name);

	if (iClassifier == null) {
	    Class classifier;
	    // Try to find it via the classpath
	    try {

		// Special case for model
		if (Model.getFacade().isAModel(mPackage)) {
		    classifier = Class.forName(namePrefix + name);
		}
		else {
                    String clazzName =
			packageJavaName + "." + namePrefix + name;
		    classifier =
			Class.forName(clazzName);
		}
		if (classifier.isInterface()) {
		    iClassifier =
			Model.getCoreFactory()
			    .buildInterface(name, mClassifier);
		}
		else {
		    if (interfacesOnly) {
		        throw new ClassNotFoundException();
		    } else {
		        iClassifier =
                                Model.getCoreFactory().buildClass(
                                        name, mClassifier);
		    }
		}
	    }
	    catch (ClassNotFoundException e) {

                // try USER classpath
                try {

                    // Special case for model
                    if (Model.getFacade().isAModel(mPackage)) {
                        classifier =
			    ImportClassLoader.getInstance()
			        .loadClass(namePrefix + name);
                    }
                    else {
                        String clazzName =
			    packageJavaName + "." + namePrefix + name;
                        classifier =
			    ImportClassLoader.getInstance()
			        .loadClass(clazzName);
                    }
                    if (classifier.isInterface()) {
                        iClassifier =
			    Model.getCoreFactory()
			        .buildInterface(name, mClassifier);
                    }
                    else {
                        if (interfacesOnly) {
                            throw new ClassNotFoundException();
                        } else {
                            iClassifier =
                                    Model.getCoreFactory().buildClass(
                                            name, mClassifier);
                        }
                    }

                }
                catch (Exception e1) {
                    // TODO: This too broad an exception catch to just continue
                    // with - narrow to specific expected errors that can be
                    // ignored - tfm
                    if (!(e1 instanceof ClassNotFoundException)) {
                        LOG.log(Level.WARNING, "Exception", e1);
                    }
                    
                    // Continue the search through the rest of the model
                    if (getContext() != null) {
                        iClassifier = getContext().get(name, interfacesOnly);
                    }
                }
	    }
	}
	return iClassifier;
    }
}

