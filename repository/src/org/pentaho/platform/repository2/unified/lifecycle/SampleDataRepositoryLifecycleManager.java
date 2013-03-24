package org.pentaho.platform.repository2.unified.lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.pentaho.database.model.DatabaseAccessType;
import org.pentaho.database.model.DatabaseConnection;
import org.pentaho.database.model.IDatabaseConnection;
import org.pentaho.database.service.IDatabaseDialectService;
import org.pentaho.database.util.DatabaseTypeHelper;
import org.pentaho.platform.api.data.IDBDatasourceService;
import org.pentaho.platform.api.engine.IAuthorizationPolicy;
import org.pentaho.platform.api.engine.IPentahoObjectFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ObjectFactoryException;
import org.pentaho.platform.api.engine.security.userroledao.IPentahoRole;
import org.pentaho.platform.api.engine.security.userroledao.IPentahoUser;
import org.pentaho.platform.api.engine.security.userroledao.IUserRoleDao;
import org.pentaho.platform.api.mt.ITenant;
import org.pentaho.platform.api.mt.ITenantManager;
import org.pentaho.platform.api.repository.datasource.DatasourceMgmtServiceException;
import org.pentaho.platform.api.repository.datasource.IDatasourceMgmtService;
import org.pentaho.platform.api.repository2.unified.IBackingRepositoryLifecycleManager;
import org.pentaho.platform.engine.core.system.PathBasedSystemSettings;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.repository2.unified.jcr.JcrTenantUtils;
import org.pentaho.platform.security.policy.rolebased.IRoleAuthorizationPolicyRoleBindingDao;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;


public class SampleDataRepositoryLifecycleManager implements IBackingRepositoryLifecycleManager{

  IDatasourceMgmtService datasourceMgmtService;
  DatabaseTypeHelper databaseTypeHelper;
  private static final String SAMPLE_DATA = "SampleData"; //$NON-NLS-1$
  private static final String DBMETA_HOSTNAME = "localhost"; //$NON-NLS-1$
  private static final String DBMETA_TYPE = "Hypersonic"; //$NON-NLS-1$
  private static final DatabaseAccessType DBMETA_ACCESS = DatabaseAccessType.NATIVE;
  private static final String DBMETA_DBNAME = SAMPLE_DATA; 
  private static final String DBMETA_PORT = "9001"; //$NON-NLS-1$
  private static final String DBMETA_USERNAME = "pentaho_user"; //$NON-NLS-1$
  private static final String DBMETA_PASSWORD = "password"; //$NON-NLS-1$
  private static final String DBMETA_ATTR_MAX_ACTIVE_VALUE = "20"; //$NON-NLS-1$
  private static final String DBMETA_ATTR_MAX_IDLE_VALUE = "5"; //$NON-NLS-1$
  private static final String DBMETA_ATTR_MAX_WAIT_VALUE = "1000"; //$NON-NLS-1$
  private static final String DBMETA_ATTR_QUERY_VALUE = "select count(*) from INFORMATION_SCHEMA.SYSTEM_SEQUENCES"; //$NON-NLS-1$
  private PathBasedSystemSettings settings = null;

  ITenantManager tenantManager;
  IUserRoleDao userRoleDao;
  String singleTenantAdminUserName;
  String tenantAdminRoleName;
  String authenticatedRoleName;
  protected String repositoryAdminUsername;
  protected IRoleAuthorizationPolicyRoleBindingDao roleBindingDao;

  
  public SampleDataRepositoryLifecycleManager(IDatasourceMgmtService datasourceMgmtService, 
                                              IDatabaseDialectService databaseDialectService,
                                              final String repositoryAdminUsername,
                                              final String singleTenantAdminUserName,
                                              final String tenantAdminRoleName,
                                              final String authenticatedRoleName,
                                              final IRoleAuthorizationPolicyRoleBindingDao roleBindingDao) {
    super();
    this.databaseTypeHelper = new DatabaseTypeHelper(databaseDialectService.getDatabaseTypes());
    this.datasourceMgmtService = datasourceMgmtService;
    this.repositoryAdminUsername = repositoryAdminUsername;
    this.singleTenantAdminUserName = singleTenantAdminUserName;
    this.tenantAdminRoleName = tenantAdminRoleName;
    this.authenticatedRoleName = authenticatedRoleName;
    this.roleBindingDao = roleBindingDao;
    settings = new PathBasedSystemSettings();
  }

  @Override
  public void startup() {
    loginAsRepositoryAdmin();
    createDefaultUsersAndRoles(JcrTenantUtils.getDefaultTenant());
  }

  @Override
  public void shutdown() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void newTenant(final ITenant tenant) {
    try {
      SecurityHelper.getInstance().runAsSystem(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          try {
            IDatabaseConnection databaseConnection = datasourceMgmtService.getDatasourceByName(DBMETA_DBNAME);
            if(databaseConnection == null) {
              createDatasource();
            }
          } catch (DatasourceMgmtServiceException dmse) {
            createDatasource();
          }
          return null;
        }
      });
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void newTenant() {
    newTenant(JcrTenantUtils.getTenant());
  }

  @Override
  public void newUser(final ITenant tenant, String username) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void newUser() {
    // TODO Auto-generated method stub
    
  }
  
 
  private IDatabaseConnection createDatabaseconnection() throws Exception {
    IDatabaseConnection databaseConnection = new DatabaseConnection();
    databaseConnection.setName(settings.getSystemSetting("sampledata-datasource/name", SAMPLE_DATA)); //$NON-NLS-1$
    databaseConnection.setHostname(settings.getSystemSetting("sampledata-datasource/host", DBMETA_HOSTNAME)); //$NON-NLS-1$
    databaseConnection.setDatabaseType(databaseTypeHelper.getDatabaseTypeByName(settings.getSystemSetting("sampledata-datasource/type", DBMETA_TYPE))); //$NON-NLS-1$
    databaseConnection.setAccessType(DatabaseAccessType.valueOf(settings.getSystemSetting("sampledata-datasource/access", DBMETA_ACCESS.toString())));
    databaseConnection.setDatabaseName(settings.getSystemSetting("sampledata-datasource/name", DBMETA_DBNAME)); //$NON-NLS-1$
    databaseConnection.setDatabasePort(settings.getSystemSetting("sampledata-datasource/port", DBMETA_PORT)); //$NON-NLS-1$
    databaseConnection.setUsername(settings.getSystemSetting("sampledata-datasource/username", DBMETA_USERNAME)); //$NON-NLS-1$
    databaseConnection.setPassword(settings.getSystemSetting("sampledata-datasource/password", DBMETA_PASSWORD)); //$NON-NLS-1$
    databaseConnection.getAttributes().put(IDBDatasourceService.MAX_ACTIVE_KEY, 
        settings.getSystemSetting("sampledata-datasource/max-active", DBMETA_ATTR_MAX_ACTIVE_VALUE)); //$NON-NLS-1$
    databaseConnection.getAttributes().put(IDBDatasourceService.MAX_IDLE_KEY,
        settings.getSystemSetting("sampledata-datasource/max-idle", DBMETA_ATTR_MAX_IDLE_VALUE)); //$NON-NLS-1$
    databaseConnection.getAttributes().put(IDBDatasourceService.MAX_WAIT_KEY,
        settings.getSystemSetting("sampledata-datasource/max-wait", DBMETA_ATTR_MAX_WAIT_VALUE)); //$NON-NLS-1$
    databaseConnection.getAttributes().put(IDBDatasourceService.QUERY_KEY, 
        settings.getSystemSetting("sampledata-datasource/query", DBMETA_ATTR_QUERY_VALUE)); //$NON-NLS-1$
    return databaseConnection;
  }

  private void createDatasource() {
    try {
      datasourceMgmtService.createDatasource(createDatabaseconnection());
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  /**
   * Logs in with given username.
   *
   * @param username username of user
   * @param tenantId tenant to which this user belongs
   * @tenantAdmin true to add the tenant admin authority to the user's roles
   */
  private void login(final String username, final ITenant tenant, final String[] roles) {
    StandaloneSession pentahoSession = new StandaloneSession(username);
    pentahoSession.setAuthenticated(tenant.getId(), username);
    PentahoSessionHolder.setSession(pentahoSession);
    pentahoSession.setAttribute(IPentahoSession.TENANT_ID_KEY, tenant.getId());
    final String password = "password";

    List<GrantedAuthority> authList = new ArrayList<GrantedAuthority>();

    for (String role : roles) {
      authList.add(new GrantedAuthorityImpl(role));
    }
    GrantedAuthority[] authorities = authList.toArray(new GrantedAuthority[0]);
    UserDetails userDetails = new User(username, password, true, true, true, true, authorities);
    Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, password, authorities);
    PentahoSessionHolder.setSession(pentahoSession);
    // this line necessary for Spring Security's MethodSecurityInterceptor
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  protected void loginAsRepositoryAdmin() {
    StandaloneSession pentahoSession = new StandaloneSession(repositoryAdminUsername);
    pentahoSession.setAuthenticated(repositoryAdminUsername);
    final GrantedAuthority[] repositoryAdminAuthorities = new GrantedAuthority[]{};
    final String password = "ignored";
    UserDetails repositoryAdminUserDetails = new User(repositoryAdminUsername, password, true, true, true, true,
        repositoryAdminAuthorities);
    Authentication repositoryAdminAuthentication = new UsernamePasswordAuthenticationToken(repositoryAdminUserDetails,
        password, repositoryAdminAuthorities);
    PentahoSessionHolder.setSession(pentahoSession);
    // this line necessary for Spring Security's MethodSecurityInterceptor
    SecurityContextHolder.getContext().setAuthentication(repositoryAdminAuthentication);
  }
  
  /**
   * @return the {@link IBackingRepositoryLifecycleManager} that this instance will use. If none has been specified,
   * it will default to getting the information from {@link PentahoSystem.get()}
   */
  public ITenantManager getTenantManager() {
    // Check ... if we haven't been injected with a lifecycle manager, get one from PentahoSystem
    try {
      IPentahoObjectFactory objectFactory = PentahoSystem.getObjectFactory();
      IPentahoSession pentahoSession = PentahoSessionHolder.getSession();
      return (null != tenantManager ? tenantManager : objectFactory.get(ITenantManager.class, "tenantMgrProxy", pentahoSession));
    } catch (ObjectFactoryException e) {
      return null;
    }
  }

  /**
   * Sets the {@link IBackingRepositoryLifecycleManager} to be used by this instance
   * @param lifecycleManager the lifecycle manager to use (can not be null)
   */
  public void setTenantManager(final ITenantManager tenantManager) {
    assert(null != tenantManager);
    this.tenantManager = tenantManager;
  }
  
  /**
   * 
   */
  private void createDefaultUsersAndRoles(ITenant defaultTenant) {

    IPentahoRole role = userRoleDao.getRole(defaultTenant, "Administrator");
    if (role == null) {
      userRoleDao.createRole(defaultTenant, "Administrator", "", new String[0]);
    }

      role = userRoleDao.getRole(defaultTenant, "Power User");
      if (role == null) {
          userRoleDao.createRole(defaultTenant, "Power User", "", new String[0]);
          roleBindingDao.setRoleBindings(defaultTenant, "Power User", Arrays.asList(new String[]{IAuthorizationPolicy.MANAGE_SCHEDULING}));
      }

      role = userRoleDao.getRole(defaultTenant, "Report Author");
      if (role == null) {
          userRoleDao.createRole(defaultTenant, "Report Author", "", new String[0]);
          roleBindingDao.setRoleBindings(defaultTenant, "Report Author", Arrays.asList(new String[]{}));
      }

      role = userRoleDao.getRole(defaultTenant, "Business Analyst");
      if (role == null) {
          userRoleDao.createRole(defaultTenant, "Business Analyst", "", new String[0]);
          roleBindingDao.setRoleBindings(defaultTenant, "Business Analyst", Arrays.asList(new String[]{IAuthorizationPolicy.MANAGE_SCHEDULING/*, IAuthorizationPolicy.PUBLISH*/}));
      }

      IPentahoUser user = userRoleDao.getUser(defaultTenant, "suzy");
      if (user == null) {
          userRoleDao.createUser(defaultTenant, "suzy", "password", "user", new String[] {authenticatedRoleName, "Power User"});
      }

      user = userRoleDao.getUser(defaultTenant, "pat");
      if (user == null) {
          userRoleDao.createUser(defaultTenant, "pat", "password", "user", new String[] {authenticatedRoleName, "Business Analyst"});
      }

      user = userRoleDao.getUser(defaultTenant, "tiffany");
      if (user == null) {
          userRoleDao.createUser(defaultTenant, "tiffany", "password", "user", new String[] {authenticatedRoleName, "Report Author"});
      }

      user = userRoleDao.getUser(defaultTenant, "admin");
      if (user == null) {
          userRoleDao.createUser(defaultTenant, "admin", "password", "user", new String[] {tenantAdminRoleName, authenticatedRoleName, "Administrator"});
      }

  }
  
  public IUserRoleDao getUserRoleDao() {
    return userRoleDao;
  }

  public void setUserRoleDao(IUserRoleDao userRoleDao) {
    this.userRoleDao = userRoleDao;
  }

}
