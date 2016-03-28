/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.subutai.plugin.appscale.impl;


import io.subutai.webui.api.WebuiModule;


/**
 *
 * @author caveman
 * @author Beyazıt Kelçeoğlu
 */
public class AppScaleWebModule implements WebuiModule
{

    public static String NAME = "AppScale";
    public static String IMG = "plugins/appscale/appscale.png";


    @Override
    public String getName()
    {
        return NAME;
    }


    @Override
    public String getModuleInfo()
    {
        return String.format( "{\"img\" : \"%s\", \"name\" : \"%s\"}", IMG, NAME );
    }


    @Override
    public String getAngularDependecyList()
    {
        return ".state('appscale', {\n" + "            url: '/plugins/appscale',\n"
                + "            templateUrl: 'plugins/appscale/partials/view.html',\n"
                + "            data: {\n"
				+ "                bodyClass: '',\n"
				+ "                layout: 'default'\n"
				+ "            },\n"
                + "            resolve: {\n"
                + "                loadPlugin: ['$ocLazyLoad', function ($ocLazyLoad) {\n"
                + "                    return $ocLazyLoad.load([\n" + "                        {\n"
                + "                            name: 'subutai.plugins.appscale',\n"
                + "                            files: [\n"
                + "                                'plugins/appscale/appscale.js',\n"
                + "                                'plugins/appscale/controller.js',\n"
                + "                                'plugins/appscale/service.js',\n"
                + "                                'subutai-app/environment/service.js'\n"
                + "                            ]\n" + "                        }\n" + "                    ]);\n"
                + "                }]\n" + "            }\n" + "        })";
    }
}

