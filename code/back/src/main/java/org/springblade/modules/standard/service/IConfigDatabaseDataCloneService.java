package org.springblade.modules.standard.service;

import org.springblade.core.mp.support.Query;
import org.springblade.modules.standard.vo.ConfigDatabaseDataClonePageVO;
import org.springblade.modules.standard.vo.DatabaseCloneVO;

import java.sql.SQLException;

public interface IConfigDatabaseDataCloneService {

	ConfigDatabaseDataClonePageVO page(Long datasourceId,String tableName, Query query) throws SQLException;

    boolean doSync(DatabaseCloneVO databaseCloneVO) throws SQLException;
}
