/**
 *  Copyright 2008 Tekton Partners
 *   All Rights Reserved
 * 
 * 
 * 
 * 
 *    author: pbk
 * 
 */

package com.tektonpartners.util;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;


public class Database extends Object {
	static Logger logger_ = Logger.getLogger(Database.class) ;
	private static boolean inited_ = false ;
	private static boolean ok_ = false ;
	private static DataSource ds_ = null ;
	private boolean lastopok_ = false;
	private Exception e_ = null ;
	private boolean adaptive_ = true ;
	private boolean cache_metadata_ = false ;
	private boolean convert_data_ = true ;
	private static int seqAppetite_ = 1000 ;
	private static int seqCurrent_ = 0 ;
	private static int seqMax_ = 0 ;
	private static int numConnections_ = 0 ;
	private static int maxConnections_ = 0 ;
	
	private static Database adminDb_ = null ;
	private String name_ = null ;
	private String dataSourceString_ = "jca:/console.dbpool/pass/JCAManagedConnectionFactory/pass" ;
	public Database() { newDb("") ; }
	public Database(String name) { newDb( name ) ; }
	public boolean debug_ = true;
    private static HashMap<String, ArrayList<String>> pkCache_ = null;

	public void newDb(String name) {
		name_ = name ;
		if ( !inited_ ) 
			init() ;
		
		logger_.info( "new db connection("+name_+")    status is ok="+ok_+"   seq="+seqCurrent_+"-"+seqMax_) ;		
		} 
	
	public void destroy() {
	}


	// TODO fix this for multi threading
	private void reserveSequences() {
		Random randy = new Random() ;
		Connection con = null ;

		int actualRequest = seqAppetite_ + randy.nextInt(seqAppetite_) ;  
		int r = adminDb_.executeUpdate("UPDATE pp_sequences SET avail=avail+"+actualRequest ) ;
		if ( r == 1 ) {
			seqMax_ = adminDb_.getInt("SELECT avail FROM pp_sequences" ) - 1 ;
			seqCurrent_ = seqMax_ - actualRequest + 1 ;
			logger_.info( "new sequence range acquired("+seqCurrent_+"-"+seqMax_+")") ;		
			}	
		else 
			logger_.error( "reserveSequences() returned "+r+" rows") ;		

		
		if ( con != null ) {
			try { con.close(); } 
			catch (SQLException e) { logger_.error(e.getMessage()); } 				
			}

		}

	private Integer getInt(String query) {
		Integer val = null ;
		Statement stmt = null ;
		Connection con = null ;
		ResultSet rs = null ;
		
		try {
			con = ds_.getConnection();
			} 
		catch (SQLException e1) { logger_.error(e1.getMessage() ) ;		}
		
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			if ( rs != null ) {
				if ( rs.first() ) {
					val = rs.getInt(1) ;
					}
				}
			}
		catch (SQLException e) {logger_.error( "Trying to getInt ("+query+") - "+e.getMessage() )	 ;}
		finally { 
			if (stmt != null) { 
				try { stmt.close(); } 
				catch (SQLException e) { logger_.error(e.getMessage()); } 
				}
			if ( con != null ) {
				try { con.close(); } 
				catch (SQLException e) { logger_.error(e.getMessage()); } 				
				}

		}

		return val;
	}

	/*
	 * 
	 * Manage Connections from the pool
	 * 
	 */
	public int getMaxConnections() {
	      synchronized (this) {
		      return maxConnections_ ;
		      }
	}

	public void resetMaxConnections() {
	      synchronized (this) {
		      maxConnections_ = 0 ;
		      }
	}

	private void destroyConnection(Connection con) {
	      synchronized (this) {
		      --numConnections_ ;
		      }
	      if ( con != null ) {
	    	  try {
	    		  con.close();
	    		  } 
	    	  catch (SQLException e) { logger_.error("destroyConnection() "+e.getMessage()) ; }
	    	  }
		else
	    	  logger_.warn("destroyConnection() received null connection ") ;
	}

	private static Connection getConnection(DataSource ds) {
	      synchronized (pkCache_) {
		      if ( ++numConnections_ >maxConnections_ ) {
		    	  maxConnections_ = numConnections_ ;
		    	logger_.info("max connections at "+maxConnections_) ;  
		      	}
		      }


		try {
			return ds.getConnection() ;
		} catch (SQLException e) { 
			logger_.error("getting connection - "+e.getMessage()) ;
			return null ;
		}
	}

	/*
	 * 
	 * Updates
	 * 
	 * 
	 */
	
	public int executeUpdate(String sql) {
		return executeUpdate( sql, false ) ;
		}

	public int executeInsert(String sql) {
		Connection con = null ;
		int returnValue = 0 ;
		con = getConnection(ds_);
		if ( con != null ) {
			returnValue = executeInsert( sql, true, con ) ;
			}
		else 
			logger_.error("failed to get connection ");
		
		destroyConnection ( con ) ;
		
		return returnValue ;
		}

	public int executeInsert(String sql, boolean autocommit, Connection con) {
		if ( debug_ )
			logger_.info("executeInsert("+sql+","+con.toString()+")" ) ;
		
		ResultSet rs = null;
		Statement stmt = null;
		logger_.info("Value of OK is=="+ok_);
		if ( ok_ ) {
			try {
				con.setAutoCommit(false) ;
				stmt = con.createStatement();
				stmt.execute(sql) ;
			    int insertedKey = -1;
			    rs = stmt.getGeneratedKeys();

			    if (rs.next()) {
			        insertedKey = rs.getInt(1);
				    logger_.info("generated key "+insertedKey ) ;
			    	} 
			    else {
			    	logger_.error("failed to get generated key for "+sql);
			    	}

				if ( autocommit ) 
					con.commit() ;
				return insertedKey ;
				}
			catch (SQLException e) { logger_.error( "Trying to executeUpdate ("+sql+") - "+e.getMessage() )	 ;	}
			finally { 
				if (stmt != null) {
					try { stmt.close(); } 
					catch (SQLException e) { logger_.error(e.getMessage()); } 
					}
				}
			}
		else
			logger_.error("Unable to execute update "+sql);

		return 0 ;
		}

	
	
	
	
	
	public int executeUpdate(String sql, boolean autocommit) {
		Connection con = null ;
		int returnValue = 0 ;
		con = getConnection(ds_);
		
		if ( con != null ) 
			returnValue = executeUpdate( sql, autocommit, con ) ;
		else 
			logger_.error("failed to get connection ");
		
		destroyConnection ( con ) ;
		
		return returnValue ;
		}
	
	public int executeUpdate(String sql, boolean autocommit, Connection con) {
		if ( debug_ )
			logger_.info("executeUpdate("+sql+")" ) ;

		Statement stmt = null;
		if ( ok_ ) {
			try {
				con.setAutoCommit(false) ;
				stmt = con.createStatement();
				if ( !stmt.execute(sql) ) {
					int updated = stmt.getUpdateCount() ;
					logger_.info("updated "+updated+" rows" ) ;
					if ( autocommit ) 
						con.commit() ;
					return updated ;
					}
				else { 
					ResultSet rs = stmt.getResultSet() ;
					ResultSetMetaData md = rs.getMetaData() ;
					
					logger_.warn("executeUpdate returned result set with "+md.getColumnCount()+" columns");
					}
				SQLWarning warn = stmt.getWarnings() ;
				if ( warn != null )
					logger_.warn("executeUpdate()"+warn.getMessage()) ;
				} 
			catch (SQLException e) { logger_.error( "Trying to executeUpdate ("+sql+") - "+e.getMessage() )	 ;	}
			finally { 
				if (stmt != null) {
					try { stmt.close(); } 
					catch (SQLException e) { logger_.error(e.getMessage()); } 
					}
				}
			}
		else
			logger_.error("Unable to execute update "+sql);

		return 0 ;
		}

	public void finalize() {
		}

	public boolean init() {
		DatabaseMetaData dbmd = null ;
		Connection con = null ;
        
		if (pkCache_ == null) {
            pkCache_ = new HashMap<String,ArrayList<String>>();
        	}
        
		if ( !inited_ ) {
			logger_.info("Initializing database module...") ;
			try {
				InitialContext ic = new InitialContext();

				ds_ = (DataSource) ic.lookup(dataSourceString_);
				if ( ds_ == null ) 
					logger_.info("NO DATABASE") ;
				else {
					logger_.info("Data Source initialized") ;
					con = ds_.getConnection();
					logger_.info("Connection established") ;
					dbmd = con.getMetaData() ;
					
					logger_.info( "Database URL: "+dbmd.getURL()) ;
					logger_.info( "Database User: "+dbmd.getUserName()) ;
					logger_.info( "DB: "+dbmd.getDatabaseProductName()+" "+dbmd.getDatabaseProductVersion()) ;
					logger_.info( "JDBC Version: "+dbmd.getJDBCMajorVersion()+"."+dbmd.getJDBCMinorVersion()) ;
					logger_.info( "Driver: "+dbmd.getDriverName()+" "+dbmd.getDriverVersion()) ;
					}
				ok_ = (ds_ != null ) && (con != null ) ;
				
				}
			catch (NamingException e) {	logger_.error(e.getMessage());	} 
			catch (SQLException e) { logger_.error(e.getMessage());	} 
			finally {
				if ( con != null ) {
					try { con.close(); } 
					catch (SQLException e) { logger_.error(e.getMessage()); } 				
					}
				}
			inited_ = true ;
			logger_.info("Db inited.  Status was ok="+ok_) ;
			}
		return ok_ ;
		}
/*
 * 
 * Queries
 * 
 * 
 */
	public DatabaseResult executeQuery ( String query ) {
		Connection con = getConnection(ds_) ;
		DatabaseResult retValue = executeQuery( query, con ) ;
		destroyConnection(con) ;
		return retValue ;
	}

	public DatabaseResult executeQuery ( String query, Connection con ) {
		if ( debug_ )
			logger_.info("executeQuery("+query+")" ) ;
		DatabaseResult value = new DatabaseResult() ;
		DataSet dataSet = new DataSet() ;
		HashMap<String, String> metaData =  new HashMap<String, String>() ;

		Statement stmt = null;

		int row = 0 ;
		int col = 0 ;
		ResultSet rs = null ;
		ResultSetMetaData md = null ;
		String columnName = "" ;
		
		if ( ok_ ) {
			try {
				DatabaseRow dataRow = null ;

				e_ = null ;
				stmt = con.createStatement();
				rs = stmt.executeQuery(query);
				
				md = rs.getMetaData() ;
				while(rs.next()) {
					dataRow = new DatabaseRow() ;
					++row ;
					//logger_.info("processing row "+row) ;
					
					for ( col = 1 ; col <= md.getColumnCount() ; col++ ) {
						if ( row == 1 ) 
							metaData.put( md.getColumnName(col), md.getColumnClassName(col) ) ;
						columnName = md.getColumnName(col) ;
						switch ( md.getColumnType(col) ) {
							case -5: dataRow.put( columnName, rs.getInt(col) ) ;			break ; //		logger_.info(md.getColumnName(col)+"("+md.getColumnClassName(col)+")   Data:"+rs.getInt(col)) ;		break ;
							case -1: dataRow.put( columnName, rs.getString(col) ) ; 		break ; //		logger_.info(md.getColumnName(col)+"("+md.getColumnClassName(col)+")   Data:"+rs.getString(col)) ;		break ;  // TEXT
							case 1: dataRow.put( columnName, rs.getString(col) ) ; 			break ; //		logger_.info(md.getColumnName(col)+"("+md.getColumnClassName(col)+")   Data:"+rs.getString(col)) ;		break ;  // TEXT
							case 3: dataRow.put( columnName, rs.getInt(col) ) ; 			break ; //		logger_.info(md.getColumnName(col)+"("+md.getColumnClassName(col)+")   Data:"+rs.getInt(col)) ;		break ;
							case 8: dataRow.put( columnName, rs.getDouble(col) ) ; 			break ; //		logger_.info(md.getColumnName(col)+"("+md.getColumnClassName(col)+")   Data:"+rs.getDouble(col)) ;		break ;
							case 4: dataRow.put( columnName, rs.getInt(col) ) ; 			break ; //		logger_.info(md.getColumnName(col)+"("+md.getColumnClassName(col)+")   Data:"+rs.getInt(col)) ;		break ;
							case 12: dataRow.put( columnName, rs.getString(col) ) ; 		break ; //		logger_.info(md.getColumnName(col)+"("+md.getColumnClassName(col)+")   Data:"+rs.getString(col)) ;		break ;
							case 93: dataRow.put( columnName, rs.getTimestamp(col) ) ; 		break ; //		logger_.info(md.getColumnName(col)+"("+md.getColumnClassName(col)+")   Data:"+rs.getTimestamp(col)) ;		break ;
							default:
								logger_.error("  Column "+col+"("+columnName+") Type:"+md.getColumnTypeName(col)+"("+md.getColumnType(col)+") was not coded.");
							}
						}
					dataSet.add(dataRow) ;
					}
				} 
			catch (SQLException e) { setError(e);	 } 
			finally { 
				if (rs != null) try { rs.close(); } catch (SQLException e) { logger_.error("ROW:"+row+"   COL:"+columnName+" - "+e.getMessage()); }
				if (stmt != null) try { stmt.close(); } catch (SQLException e) { logger_.error("ROW:"+row+"   COL:"+columnName+" - "+e.getMessage()); } 
				}
			}
		else
			logger_.error("Unable to execute query "+query);
		logger_.info("Rows: "+row) ;
		value.put("metadata", metaData) ;
		value.put("dataset", dataSet) ;
		return value;
		}
	


	
	public boolean executeQuery ( String query, PrintWriter output ) { 		
		Connection con = getConnection(ds_) ;
		logger_.info("Execute query "+query);
		executeQuery( query, output, con ) ;
		destroyConnection(con) ;
		return true ;
		}

	

	public boolean executeQuery ( String query, PrintWriter output, Connection con ) {
		Statement stmt = null;
		boolean value = false ;
		ResultSet rs = null ;
		String columnName = "" ;

		if ( ok_ ) {
			try {
				e_ = null ;
				int row = 0 ;
				stmt = con.createStatement();
				rs = stmt.executeQuery(query);
				ResultSetMetaData md = rs.getMetaData() ;
				output.printf( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" ) ;
//				output.printf("<?xml-stylesheet type=\"text/css\" href=\"dev1.payparade.net/css/payparade_xml_data.css\"?>") ;
				output.printf( "<data>\n" ) ;
				while(rs.next()) {
					logger_.trace("Row "+(++row));
					output.printf( "\t<%s>\n", md.getTableName(1) ) ;
					for ( int col = 1 ; col <= md.getColumnCount() ; col++ ) {
						logger_.trace("  Column "+col+" "+md.getColumnTypeName(col)+"("+md.getColumnType(col)+")");
						output.printf( "\t\t<%s>", md.getColumnName(col) ) ;
						try {
							columnName = md.getColumnName(col) ;
							switch ( md.getColumnType(col) ) {
								case -5: output.printf( "%d", rs.getInt(col) ) ; break ;  // TODO this is big int - improve handling
								case -1: output.printf( "%s", rs.getString(col) ) ; break ;
								case 1: output.printf( "%s", rs.getString(col) ) ; break ;
								case 4: output.printf( "%d", rs.getInt(col) ) ; break ;
								case 8: output.printf( "%f", rs.getDouble(col) ) ; break ; //		logger_.info(md.getColumnName(col)+"("+md.getColumnClassName(col)+")   Data:"+rs.getDouble(col)) ;		break ;
								case 12: output.printf( "%s", rs.getString(col) ) ; break ;
								case 93: output.printf( "%s", rs.getTimestamp(col) ) ; 	break ; //		logger_.info(md.getColumnName(col)+"("+md.getColumnClassName(col)+")   Data:"+rs.getTimestamp(col)) ;		break ;
								default:
									logger_.error("  Column "+col+"("+columnName+") Type:"+md.getColumnTypeName(col)+"("+md.getColumnType(col)+") was not coded.");
								}
							}
						catch (SQLException e) { setError(e);	 } 							
						output.printf( "</%s>\n", md.getColumnName(col) ) ;
						}
					output.printf( "\t</%s>\n", md.getTableName(1) ) ;
					value = true ;
					}
				output.printf( "</data>\n" ) ;
				logger_.info("retrieved "+row+" rows") ;
				} 
			catch (SQLException e) { setError(e);	 } 
			finally { 
				if (rs != null) try { rs.close(); } catch (SQLException e) { logger_.error(e.getMessage()); }
				if (stmt != null) try { stmt.close(); } catch (SQLException e) { logger_.error(e.getMessage()); } 
				}
			}
		else
			logger_.error("Unable to execute query "+query);

		return value;
		}
	



	private void setError(Exception e) {
		e_ = e ;
		lastopok_ = false ;
		logger_.error(e_.getMessage()) ;		
	}

	public String getLastError() { 
		return "db ok="+ok_+"  last error was "+(( e_ == null )?"Unknown Error":e_.getMessage() ) ;		
	}


	private String getTypeName( String columnName ) {
		if ( columnName.endsWith("_id") ) return "TEXT" ;
		else if ( columnName.endsWith("_count") ) return "INT" ;
		else if ( columnName.endsWith("_num") ) return "INT" ;
		else if ( columnName.endsWith("_url") ) return "TEXT" ;
		else if ( columnName.endsWith("_date") ) return "TEXT" ;
		else if ( columnName.endsWith("_at") ) return "TEXT" ;
		else return "TEXT" ;
		}

	private Object getTypedData( String columnName, String data ) {
		if ( getTypeName(columnName).equals("TEXT") ) return data ;
		else if ( getTypeName(columnName).equals("INT") ) return new Integer(data) ;
		else if ( getTypeName(columnName).equals("TIMESTAMP") ) return new Timestamp(new Long(data)) ;
		else if ( getTypeName(columnName).equals("DATE") ) {
			return new Date(new Long(data)) ;
			}
		return null ;
		}
	

	private void loadMetadata(String tableName) {		
	}

	public synchronized String getNewID() { 
		if ( ++seqCurrent_ > seqMax_ ) 
			reserveSequences() ;
		return ""+seqCurrent_ ;
	}

	public static boolean isKey(String table, String column) {
        ArrayList<String> keyNames = null ;
        String tableName = table.toUpperCase() ;
        String columnName = column.toUpperCase() ;
		logger_.info("isKey("+tableName+"."+column+")");
		ArrayList<String> keys = getKeyFields(tableName) ;
		return keys.contains(columnName) ;
		}
	
	public static ArrayList<String> getKeyFields(String tableName) {
        ArrayList<String> keyNames = null ;
        tableName = tableName.toUpperCase() ;
		logger_.info("getKeyFields("+tableName+")");
		Connection con = getConnection(ds_) ;


		synchronized (pkCache_) {
			keyNames = pkCache_.get(tableName) ;
			if (keyNames == null || keyNames.isEmpty() ) {
				Connection conn = null;
				DatabaseMetaData md = null ;
				keyNames = new ArrayList<String>() ;
				try {
					md = con.getMetaData();
			        ResultSet table = md.getTables(null, null, tableName, null);
			        if ( table.next() ) {
			    		logger_.trace("getKeyFields() found: "+table.getString("TABLE_SCHEM")+"."+table.getString("TABLE_NAME"));
			        	
			    		ResultSet primaryKeys = md.getPrimaryKeys(null, null,table.getString("TABLE_NAME"));
			    		while (primaryKeys.next()) {
			    			String primaryKeyColumn = primaryKeys.getString("COLUMN_NAME");
			    			keyNames.add(primaryKeyColumn.toUpperCase()) ;
			    			logger_.trace("Primary Key Column: " + primaryKeyColumn);
    						}
			        	}
					pkCache_.put(tableName, keyNames);
    				} 
				catch (SQLException e) { logger_.error("SQL ex "+e.getMessage()); }
				finally {
					if (conn != null) {
						try {
							conn.close();
							} catch (SQLException e) {/* do nothing */}
	        			}
					}
				conn = null ;
    			}
			}
		return keyNames ;
		}

	

}
