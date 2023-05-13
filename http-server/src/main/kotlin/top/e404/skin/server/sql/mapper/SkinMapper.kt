package top.e404.skin.server.sql.mapper

import org.apache.ibatis.annotations.*
import top.e404.skin.server.sql.pojo.SkinData

interface SkinMapper {
    /**
     * 初始化表
     */
    @Update(
        """
        CREATE TABLE IF NOT EXISTS skin
        (
            `uuid`   CHAR(36)        NOT NULL PRIMARY KEY COMMENT '玩家uuid, 唯一且不可变',
            `name`   VARCHAR(16)     NOT NULL COMMENT '玩家名, 可变',
            `slim`   BOOLEAN         NOT NULL COMMENT '玩家皮肤模型',
            `update` BIGINT UNSIGNED NOT NULL COMMENT '最后更新时间',
            `hash` CHAR(64) NOT NULL COMMENT '用于获取皮肤的url后的hash值'
        ) ENGINE InnoDB
          DEFAULT CHARSET UTF8MB4
    """
    )
    fun initTable()

    /**
     * 添加新的记录
     */
    @Insert("REPLACE INTO `skin` VALUE (#{uuid}, #{name}, #{slim}, #{update}, #{hash})")
    fun addRow(
        @Param("uuid") uuid: String,
        @Param("name") name: String,
        @Param("slim") slim: Boolean,
        @Param("update") update: Long,
        @Param("hash") hash: String,
    ): Long

    /**
     * 添加新的记录
     */
    @Select("REPLACE INTO `skin` VALUE (#{data.uuid}, #{data.name}, #{data.slim}, #{data.update}, #{data.hash})")
    fun add(@Param("data") data: SkinData)

    /**
     * 通过名字获取
     */
    @Select("SELECT uuid, name, slim, `update`, hash FROM `skin` WHERE name = #{name}")
    @ResultType(SkinData::class)
    fun getByName(name: String): SkinData?

    /**
     * 通过id获取
     */
    @Select("SELECT uuid, name, slim, UNIX_TIMESTAMP(`update`) FROM `skin` WHERE uuid = #{id}")
    @ResultType(String::class)
    fun getById(id: String): SkinData?

    /**
     * 获取超过有效期的用户信息
     */
    @Select("SELECT uuid FROM skin WHERE UNIX_TIMESTAMP(`update`) < #{timestamp}")
    @ResultType(String::class)
    fun getTimeout(timestamp: Long): MutableList<String>

    /**
     * 删除用户
     */
    @Delete(
        """
        <script>
        DELETE FROM skin
        WHERE uuid in
        <foreach item="item" collection="list" open="(" separator="," close=")">
            #{item}
        </foreach>
        </script>
    """
    )
    fun delete(uuids: MutableList<String>)
}
