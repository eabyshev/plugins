package io.subutai.plugin.appscale.api;


import java.util.ArrayList;
import java.util.List;

import io.subutai.common.settings.Common;
import io.subutai.core.plugincommon.api.ConfigBase;


/**
 *
 * @author caveman
 */
public class AppScaleConfig implements ConfigBase
{
    public static final String PRODUCT_NAME = "AppScale";
    public static final String PRODUCT_KEY = "AppScale";
    public static final String PACKAGE_NAME = ( Common.PACKAGE_PREFIX + PRODUCT_NAME ).toLowerCase ();

    // private static final String TEMPLATE_NAME = "Appscale";
    private static final String TEMPLATE_NAME = "appscale"; // we will be using master template
    private String clusterName = ""; // this will be login point => management AKA master
    // if any means of other containers..
    // private String loginName;
    private String zookeeperName;
    private String cassandraName;
    private String appengine;

    private List<String> zooList;
    private List<String> cassList;
    private List<String> appenList;


    private String domainName = "intra.lan";
    private List<String> nodes;
    private String environmentId;
    private String containerType;
    private String tracker;
    private List<AppScaleConfig> clusters;
    private List<String> clusterNames;
    private String userDomain;
    private int vlanNumber;
    private String scaleOption;


    public AppScaleConfig ()
    {
        nodes = new ArrayList<> ();
    }


    public String getClusterName ()
    {
        return this.clusterName;
    }


    public void setClusterName ( String clusterName )
    {
        this.clusterName = clusterName;
    }


    public String getProductName ()
    {
        return AppScaleConfig.PRODUCT_NAME;
    }


    public String getProductKey ()
    {
        return AppScaleConfig.PRODUCT_KEY;
    }


    public List<String> getNodes ()
    {
        return nodes;
    }


    public void setNodes ( List<String> nodes )
    {
        this.nodes = nodes;
    }


    public String getEnvironmentId ()
    {
        return environmentId;
    }


    public void setEnvironmentId ( String environmentId )
    {
        this.environmentId = environmentId;
    }


    public String getContainerType ()
    {
        return containerType;
    }


    public void setContainerType ( String containerType )
    {
        this.containerType = containerType;
    }


    public String getTracker ()
    {
        return tracker;
    }


    public void setTracker ( String tracker )
    {
        this.tracker = tracker;
    }


    public String getZookeeperName ()
    {
        return zookeeperName;
    }


    public void setZookeeperName ( String zookeeperName )
    {
        this.zookeeperName = zookeeperName;
    }


    public String getCassandraName ()
    {
        return cassandraName;
    }


    public void setCassandraName ( String cassandraName )
    {
        this.cassandraName = cassandraName;
    }


    public List<AppScaleConfig> getClusters ()
    {
        return clusters;
    }


    public void setClusters ( List<AppScaleConfig> clusters )
    {
        this.clusters = clusters;
    }


    public List<String> getclusterNames ()
    {
        return clusterNames;
    }


    public void setclusterNames ( List<String> clusterNames )
    {
        this.clusterNames = clusterNames;
    }


    public String getUserDomain ()
    {
        return userDomain;
    }


    public void setUserDomain ( String userDomain )
    {
        this.userDomain = userDomain;
    }


    public Integer getVlanNumber ()
    {
        return vlanNumber;
    }


    public void setVlanNumber ( Integer vlanNumber )
    {
        this.vlanNumber = vlanNumber;
    }


    public String getAppengine ()
    {
        return appengine;
    }


    public void setAppengine ( String appengine )
    {
        this.appengine = appengine;
    }


    public List<String> getZooList ()
    {
        return zooList;
    }


    public void setZooList ( List<String> zooList )
    {
        this.zooList = zooList;
    }


    public List<String> getCassList ()
    {
        return cassList;
    }


    public void setCassList ( List<String> cassList )
    {
        this.cassList = cassList;
    }


    public List<String> getAppenList ()
    {
        return appenList;
    }


    public void setAppenList ( List<String> appenList )
    {
        this.appenList = appenList;
    }


    public String getScaleOption ()
    {
        return scaleOption;
    }


    public void setScaleOption ( String scaleOption )
    {
        this.scaleOption = scaleOption;
    }


//    public String getLoginNode ()
//    {
//        return loginName;
//    }
//
//
//    public void setLoginNode ( String loginName )
//    {
//        this.loginName = loginName;
//    }
    public static String getPRODUCT_NAME ()
    {
        return PRODUCT_NAME;
    }


    public static String getPRODUCT_KEY ()
    {
        return PRODUCT_KEY;
    }


    public static String getPACKAGE_NAME ()
    {
        return PACKAGE_NAME;
    }


    public static String getTEMPLATE_NAME ()
    {
        return TEMPLATE_NAME;
    }


    public String getDomainName ()
    {
        return domainName;
    }


    public void setDomainName ( String domainName )
    {
        this.domainName = domainName;
    }


}

