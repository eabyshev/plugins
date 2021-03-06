package io.subutai.plugin.sqoop.impl;


import io.subutai.common.command.RequestBuilder;
import io.subutai.common.settings.Common;
import io.subutai.core.plugincommon.api.NodeOperationType;
import io.subutai.plugin.sqoop.api.SqoopConfig;
import io.subutai.plugin.sqoop.api.setting.CommonSetting;
import io.subutai.plugin.sqoop.api.setting.ExportSetting;
import io.subutai.plugin.sqoop.api.setting.ImportParameter;
import io.subutai.plugin.sqoop.api.setting.ImportSetting;


public class CommandFactory
{

    public static final String PACKAGE_NAME = Common.PACKAGE_PREFIX + SqoopConfig.PRODUCT_KEY.toLowerCase();
    private static final String EXEC_PROFILE = ". /etc/profile";


    public static String build( NodeOperationType type, CommonSetting settings )
    {
        String s = null;
        boolean use_opt = false;
        switch ( type )
        {
            case STATUS:
                s = "dpkg -l | grep '^ii' | grep " + Common.PACKAGE_PREFIX_WITHOUT_DASH;
                break;
            case INSTALL:
            case UNINSTALL:
                StringBuilder sb = new StringBuilder( "apt-get --force-yes --assume-yes " );
                sb.append( type == NodeOperationType.INSTALL ? "install" : "purge" ).append( " " );
                sb.append( PACKAGE_NAME );
                s = sb.toString();
                break;
            case IMPORT:
                if ( settings instanceof ImportSetting )
                {
                    s = importData( ( ImportSetting ) settings );
                }
                use_opt = true;
                break;
            case EXPORT:
                if ( settings instanceof ExportSetting )
                {
                    s = exportData( ( ExportSetting ) settings );
                }
                use_opt = true;
                break;
            default:
                throw new IllegalArgumentException( "Unsupported operation type: " + type );
        }
        if ( use_opt && settings != null && settings.getOptionalParameters() != null )
        {
            s += " " + settings.getOptionalParameters();
        }
        return s;
    }


    public static String fetchDatabasesQuery( ImportSetting importSetting )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( EXEC_PROFILE ).append( " && " );
        sb.append( "sqoop list-databases " );
        appendOption( sb, "username", importSetting.getUsername() );
        appendOption( sb, "password", importSetting.getPassword() );
        appendOption( sb, "connect", importSetting.getConnectionString() );
        return sb.toString();
    }


    public static String fetchTablesQuery( ImportSetting importSetting )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( EXEC_PROFILE ).append( " && " );
        sb.append( "sqoop list-tables " );
        appendOption( sb, "username", importSetting.getUsername() );
        appendOption( sb, "password", importSetting.getPassword() );
        appendOption( sb, "connect", importSetting.getConnectionString() );
        return sb.toString();
    }


    public static RequestBuilder getCheckInstalledCommand( String packageName )
    {
        String command = "dpkg -s " + packageName;
        return new RequestBuilder( command );
    }


    public static RequestBuilder getConfigureCommand()
    {
        return new RequestBuilder( "dpkg --configure -a && rm /var/cache/apt/archives/lock && rm /var/lib/dpkg/lock" );
    }


    private static String importData( ImportSetting settings )
    {
        boolean all = settings.getBooleanParameter( ImportParameter.IMPORT_ALL_TABLES );
        StringBuilder sb = new StringBuilder();
        sb.append( EXEC_PROFILE ).append( " && " );
        switch ( settings.getType() )
        {
            case HDFS:
                if ( all )
                {
                    sb.append( "sqoop-import-all-tables" );
                }
                else
                {
                    sb.append( "sqoop-import" );
                }
                appendOption( sb, "connect", settings.getConnectionString() );
                appendOption( sb, "username", settings.getUsername() );
                appendOption( sb, "password", settings.getPassword() );
                if ( !all )
                {
                    appendOption( sb, "table", settings.getTableName() );
                }
                break;
            case HBASE:
                sb.append( "sqoop import" );
                appendOption( sb, "connect", settings.getConnectionString() );
                appendOption( sb, "username", settings.getUsername() );
                appendOption( sb, "password", settings.getPassword() );
                appendOption( sb, "table", settings.getTableName() );
                appendOption( sb, "hbase-create-table", null );
                appendOption( sb, "hbase-table", settings.getStringParameter( ImportParameter.DATASOURCE_TABLE_NAME ) );
                appendOption( sb, "column-family",
                        settings.getStringParameter( ImportParameter.DATASOURCE_COLUMN_FAMILY ) );
                break;
            case HIVE:
                if ( all )
                {
                    sb.append( "sqoop-import-all-tables" );
                }
                else
                {
                    sb.append( "sqoop-import" );
                }
                appendOption( sb, "connect", settings.getConnectionString() );
                appendOption( sb, "username", settings.getUsername() );
                appendOption( sb, "password", settings.getPassword() );
                appendOption( sb, "hive-import", null );
                if ( !all )
                {
                    appendOption( sb, "table", settings.getTableName() );
                    String db = settings.getStringParameter( ImportParameter.DATASOURCE_DATABASE );
                    String tb = settings.getStringParameter( ImportParameter.DATASOURCE_TABLE_NAME );
                    appendOption( sb, "hive-table", db + "." + tb );
                }
                break;
            default:
                throw new AssertionError( settings.getType().name() );
        }
        return sb.toString();
    }


    private static void appendOption( StringBuilder sb, String option, String value )
    {
        sb.append( " --" ).append( option );
        if ( value != null )
        {
            sb.append( " " ).append( value );
        }
    }


    private static String exportData( ExportSetting settings )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( EXEC_PROFILE ).append( " && " );
        sb.append( "sqoop export" );
        appendOption( sb, "connect", settings.getConnectionString() );
        appendOption( sb, "username", settings.getUsername() );
        appendOption( sb, "password", settings.getPassword() );
        appendOption( sb, "table", settings.getTableName() );
        appendOption( sb, "export-dir", settings.getHdfsPath() );
        return sb.toString();
    }
}
