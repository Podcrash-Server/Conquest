package me.raindance.champions.db;

import me.raindance.champions.Perm;
import nu.studer.sample.Tables;
import nu.studer.sample.tables.Permissions;
import nu.studer.sample.tables.records.PermissionsRecord;
import org.jooq.Cursor;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerPermissionsTable extends BaseTable implements IPlayerDB {
    private PlayerTable players;
    private Permissions PERMISSIONS;
    public PlayerPermissionsTable(boolean test) {
        super("permissions", test);
        this.PERMISSIONS = Tables.PERMISSIONS.rename(getName());
        this.players = TableOrganizer.getTable(DataTableType.PLAYERS, test);
    }

    @Override
    public PlayerTable getPlayerTable() {
        return players;
    }

    @Override
    public void createTable() {

        getContext().createTableIfNotExists(getName())
            .column("player_id", SQLDataType.BIGINT)
            .column("permission_id", SQLDataType.INTEGER)
            .constraints(
                DSL.constraint(getConstraintPrefix() + "unique").unique("player_id", "permission_id"),
                DSL.constraint(getConstraintPrefix() + "foreign_player_id")
                    .foreignKey("player_id")
                    .references(players.getName(), "_id"))
            .execute();

        getContext().createIndexIfNotExists(getConstraintPrefix() + "player_id_index").on(getName(), "player_id")
            .execute();
    }

    @Override
    public void dropTable() {
        getContext().dropIndex(getConstraintPrefix() + "player_id_index")
            .execute();
        super.dropTable();
    }

    public void addRole(UUID uuid, Perm role) {
        getContext().insertInto(PERMISSIONS,
            PERMISSIONS.PLAYER_ID, PERMISSIONS.PERMISSION_ID)
            .values(getID(uuid), role.getDbId())
            .execute();
    }

    public boolean hasRole(UUID uuid, Perm role) {
        Cursor<Record1<Integer>> cursor = getContext().select(PERMISSIONS.PERMISSION_ID)
                .from(PERMISSIONS)
                .where(PERMISSIONS.PLAYER_ID.eq(getID(uuid)))
                .fetchLazy();
        while(cursor.hasNext()) {
            Record id = cursor.fetchNext();
            if(Perm.getBy(id.into(Integer.class)) == role) {
                cursor.close();
                return true;
            }
        }
        return false;
    }

    public List<Perm> getRoles(UUID uuid) {
        List<Perm> perms = new ArrayList<>();
        Cursor<Record1<Integer>> cursor = getContext().select(PERMISSIONS.PERMISSION_ID)
                .from(PERMISSIONS)
                .where(PERMISSIONS.PLAYER_ID.eq(getID(uuid)))
                .fetchLazy();
        while(cursor.hasNext()) {
            Record id = cursor.fetchNext();
            perms.add(Perm.getBy(id.into(Integer.class)));
        }
        return perms;
    }

    public void removeRole(UUID uuid, Perm role) {
        getContext().delete(PERMISSIONS).
            where(
                PERMISSIONS.PLAYER_ID.eq(getID(uuid)),
                PERMISSIONS.PERMISSION_ID.eq(role.getDbId()))
            .execute();
    }
}
