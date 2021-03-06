package io.subutai.plugin.zookeeper.impl;


/**
 * ZK config params
 */
public enum ConfigParams
{
    DATA_DIR( "dataDir", "/var/zookeeper" ), SERVERS( "servers", null ), PORTS( null, ":2888:3888 " ),
    MY_ID_FILE( null, "myid" ), CONFIG_FILE_PATH( null, "/etc/zookeeper/conf/zoo.cfg" );

    private final String placeHolder;
    private final String paramValue;


    ConfigParams( final String placeHolder, final String paramValue )
    {
        this.placeHolder = placeHolder;
        this.paramValue = paramValue;
    }


    public String getPlaceHolder()
    {
        return placeHolder;
    }


    public String getParamValue()
    {
        return paramValue;
    }
}

