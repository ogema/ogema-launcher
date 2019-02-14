/**
 * Copyright (c) 2016 Fraunhofer-Gesellschaft
 *                     zur Förderung der angewandten Wissenschaften e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ogema.launcher.config.xml;

import java.util.ArrayList;
import java.util.List;
public class Configuration {

    protected Bundle frameworkbundle;
    protected Configuration.Bundles bundles;
    protected Configuration.Properties properties;
    protected Configuration.DeleteList deleteList;

    /**
     * Gets the value of the frameworkbundle property.
     * 
     * @return
     *     possible object is
     *     {@link Bundle }
     *     
     */
    public Bundle getFrameworkbundle() {
        return frameworkbundle;
    }

    /**
     * Sets the value of the frameworkbundle property.
     * 
     * @param value
     *     allowed object is
     *     {@link Bundle }
     *     
     */
    public void setFrameworkbundle(Bundle value) {
        this.frameworkbundle = value;
    }

    /**
     * Gets the value of the bundles property.
     * 
     * @return
     *     possible object is
     *     {@link Configuration.Bundles }
     *     
     */
    public Configuration.Bundles getBundles() {
        return bundles;
    }

    /**
     * Sets the value of the bundles property.
     * 
     * @param value
     *     allowed object is
     *     {@link Configuration.Bundles }
     *     
     */
    public void setBundles(Configuration.Bundles value) {
        this.bundles = value;
    }

    /**
     * Gets the value of the properties property.
     * 
     * @return
     *     possible object is
     *     {@link Configuration.Properties }
     *     
     */
    public Configuration.Properties getProperties() {
        return properties;
    }

    /**
     * Sets the value of the properties property.
     * 
     * @param value
     *     allowed object is
     *     {@link Configuration.Properties }
     *     
     */
    public void setProperties(Configuration.Properties value) {
        this.properties = value;
    }

    /**
     * Gets the value of the deleteList property.
     * 
     * @return
     *     possible object is
     *     {@link Configuration.DeleteList }
     *     
     */
    public Configuration.DeleteList getDeleteList() {
        return deleteList;
    }

    /**
     * Sets the value of the deleteList property.
     * 
     * @param value
     *     allowed object is
     *     {@link Configuration.DeleteList }
     *     
     */
    public void setDeleteList(Configuration.DeleteList value) {
        this.deleteList = value;
    }

    public static class Bundles {

        protected List<Bundle> bundle;

        public List<Bundle> getBundle() {
            if (bundle == null) {
                bundle = new ArrayList<Bundle>();
            }
            return this.bundle;
        }

    }


    public static class DeleteList {

        protected List<String> file;

        /**
         * Gets the value of the file property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the file property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getFile().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link String }
         * 
         * 
         */
        public List<String> getFile() {
            if (file == null) {
                file = new ArrayList<String>();
            }
            return this.file;
        }

    }

    public static class Properties {

        protected List<Configuration.Properties.Property> property;

        /**
         * Gets the value of the property property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the property property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getProperty().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Configuration.Properties.Property }
         * 
         * 
         */
        public List<Configuration.Properties.Property> getProperty() {
            if (property == null) {
                property = new ArrayList<Configuration.Properties.Property>();
            }
            return this.property;
        }

        public static class Property {

            protected String key;
            protected String value;

            /**
             * Gets the value of the key property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getKey() {
                return key;
            }

            /**
             * Sets the value of the key property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setKey(String value) {
                this.key = value;
            }

            /**
             * Gets the value of the value property.
             * 
             * @return
             *     possible object is
             *     {@link String }
             *     
             */
            public String getValue() {
                return value;
            }

            /**
             * Sets the value of the value property.
             * 
             * @param value
             *     allowed object is
             *     {@link String }
             *     
             */
            public void setValue(String value) {
                this.value = value;
            }

        }

    }

}
