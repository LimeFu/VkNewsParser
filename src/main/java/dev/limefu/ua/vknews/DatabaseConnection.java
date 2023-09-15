package dev.limefu.ua.vknews;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.SQLDataType;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;

public class DatabaseConnection {
    private final DSLContext dslContext;



    public DatabaseConnection(DataSource dataSource) {
        ConnectionProvider connectionProvider = new DataSourceConnectionProvider(dataSource);
        Configuration configuration = new DefaultConfiguration()
                .set(connectionProvider)
                .set(SQLDialect.MYSQL);

        dslContext = DSL.using(configuration);

        createTableIfNotExists();
    }

    public DSLContext getDSLContext() {
        return dslContext;
    }

    public void close(DataSource dataSource) {
        try {
            dataSource.getConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


   
    public void createTableIfNotExists() {
        dslContext.createTableIfNotExists(DSL.table("vk_posts"))
                .column(DSL.field("id", SQLDataType.INTEGER.nullable(false).identity(true)))
                .column(DSL.field("postId", SQLDataType.VARCHAR(255).nullable(false)))
                .column(DSL.field("date", SQLDataType.TIMESTAMP.nullable(false)))
                .constraints(
                        DSL.constraint(DSL.name("pk_vk_posts")).primaryKey(DSL.field("id"))
                )
                .execute();
    }
    public void insertPost(String postId, Timestamp date) {
        dslContext.insertInto(DSL.table("vk_posts"))
                .set(DSL.field("postId"), postId)
                .set(DSL.field("date"), new Timestamp(date))
                .execute();
    }

    public Result<Record> getLatestPosts(int limit) {
        return dslContext.selectFrom(DSL.table("vk_posts"))
                .orderBy(DSL.field("date").desc())
                .limit(limit)
                .fetch();
    }
}



