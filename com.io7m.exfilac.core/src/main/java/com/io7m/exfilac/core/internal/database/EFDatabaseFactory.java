package com.io7m.exfilac.core.internal.database;

import com.io7m.darco.api.DDatabaseException;
import com.io7m.darco.sqlite.DSDatabaseFactory;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.lanark.core.RDottedName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.io.InputStream;
import java.util.List;

public final class EFDatabaseFactory extends DSDatabaseFactory<
  EFDatabaseConfiguration,
  EFDatabaseConnectionType,
  EFDatabaseTransactionType,
  EFDatabaseQueryProviderType<?, ?, ?>,
  EFDatabaseType> {

  private static final Logger LOG =
    LoggerFactory.getLogger(EFDatabaseFactory.class);

  /**
   * The main database factory.
   */

  public EFDatabaseFactory() {

  }

  @Override
  protected RDottedName applicationId() {
    return new RDottedName("com.io7m.exfilac");
  }

  @Override
  protected Logger logger() {
    return LOG;
  }

  @Override
  protected EFDatabaseType onCreateDatabase(
    final EFDatabaseConfiguration configuration,
    final SQLiteDataSource source,
    final List<EFDatabaseQueryProviderType<?, ?, ?>> queryProviders,
    final CloseableCollectionType<DDatabaseException> resources) {
    return new EFDatabase(
      configuration,
      source,
      queryProviders,
      resources
    );
  }

  @Override
  protected InputStream onRequireDatabaseSchemaXML() {
    return EFDatabaseFactory.class.getResourceAsStream(
      "/com/io7m/exfilac/core/database.xml"
    );
  }

  @Override
  protected void onEvent(
    final String message) {

  }

  @Override
  protected void onAdjustSQLiteConfig(
    final SQLiteConfig config) {

  }

  @Override
  protected List<EFDatabaseQueryProviderType<?, ?, ?>> onRequireDatabaseQueryProviders() {
    return List.of(
      EFQBucketDelete.provider(),
      EFQBucketList.provider(),
      EFQBucketPut.provider(),
      EFQSettingsGet.provider(),
      EFQSettingsPut.provider(),
      EFQUploadConfigurationDelete.provider(),
      EFQUploadConfigurationList.provider(),
      EFQUploadConfigurationPut.provider(),
      EFQUploadEventRecordAdd.provider(),
      EFQUploadEventRecordList.provider(),
      EFQUploadRecordCreate.provider(),
      EFQUploadRecordDeleteByAge.provider(),
      EFQUploadRecordGet.provider(),
      EFQUploadRecordList.provider(),
      EFQUploadRecordMostRecent.provider(),
      EFQUploadRecordUpdate.provider()
    );
  }
}
