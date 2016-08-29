package io.subutai.plugin.pig.rest.pojo;


public class ContainerPojo
{
    private String hostname;
    private String ip;


    public ContainerPojo()
    {
        hostname = "";
    }


    public ContainerPojo( final String hostname, final String ip )
    {
        this.hostname = hostname;
        this.ip = ip;
    }


    public String getHostname()
    {
        return hostname;
    }


    public void setHostname( final String hostname )
    {
        this.hostname = hostname;
    }


    public String getIp()
    {
        return ip;
    }


    public void setIp( final String ip )
    {
        this.ip = ip;
    }
}
