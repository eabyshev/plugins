package io.subutai.plugin.elasticsearch.impl;


import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import io.subutai.plugin.elasticsearch.api.ElasticsearchClusterConfiguration;
import io.subutai.webui.api.WebuiModule;
import io.subutai.webui.entity.AngularjsDependency;
import io.subutai.webui.entity.WebuiModuleResourse;


public class ElasticSearchWebModule implements WebuiModule
{
    private WebuiModuleResourse elasticsearchResource;
    private static String NAME = "ElasticSearch";
    private static String IMG = "plugins/elasticsearch/elasticsearch.png";
    private static final String SIZE = "MEDIUM";


    private static final Map<String, Integer> TEMPLATES_REQUIREMENT;

    static
    {
        TEMPLATES_REQUIREMENT = new HashMap<>();
        TEMPLATES_REQUIREMENT.put( ElasticsearchClusterConfiguration.TEMPLATE_NAME, 3 );
    }

    public void init()
    {
        this.elasticsearchResource = new WebuiModuleResourse( NAME.toLowerCase(), IMG );
        AngularjsDependency angularjsDependency =
                new AngularjsDependency( "subutai.plugins.elastic-search", "plugins/elasticsearch/elastic-search.js",
                        "plugins/elasticsearch/controller.js", "plugins/elasticsearch/service.js",
                        "subutai-app/environment/service.js" );

        this.elasticsearchResource.addDependency( angularjsDependency );
    }


    @Override
    public String getModuleInfo()
    {
        return String
                .format( "{\"img\" : \"%s\", \"name\" : \"%s\", \"size\" : \"%s\", \"requirement\" : %s}", IMG, NAME,
                        SIZE, new Gson().toJson( TEMPLATES_REQUIREMENT ).toString() );
    }


    @Override
    public String getName()
    {
        return NAME;
    }


    @Override
    public String getAngularState()
    {
        return this.elasticsearchResource.getAngularjsList();
    }


    @Override
    public String getAngularDependecyList()
    {
        return String.format( ".state('%s', %s)", NAME.toLowerCase(), this.elasticsearchResource.getAngularjsList() );
    }
}
