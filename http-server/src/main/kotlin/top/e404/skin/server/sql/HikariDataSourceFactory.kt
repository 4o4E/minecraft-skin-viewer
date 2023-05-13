package top.e404.skin.server.sql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.apache.ibatis.datasource.DataSourceFactory
import java.util.*
import javax.sql.DataSource

class HikariDataSourceFactory : DataSourceFactory {
    private lateinit var props: Properties
    override fun setProperties(props: Properties) {
        this.props = props
    }

    private var dataSource: DataSource? = null
    override fun getDataSource() = dataSource
        ?: HikariDataSource(
            HikariConfig(props)
        ).also { dataSource = it }
}
