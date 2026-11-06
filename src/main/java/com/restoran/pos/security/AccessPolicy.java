package com.restoran.pos.security;

import com.restoran.pos.entity.RestaurantTable;
import com.restoran.pos.entity.UserAccount;
import com.restoran.pos.exception.ForbiddenOperationException;
import org.springframework.stereotype.Component;

/**
 * Kayıt sahipliği denetimi: garson yalnızca kendisine atanmış masaların, müşteri yalnızca bağlı olduğu
 * masanın kayıtlarına erişir. ALL_TABLES_ACCESS yetkisine sahip roller (kasiyer, yönetici) tüm masaları görür.
 */
@Component
public class AccessPolicy {

    public boolean hasAllTablesAccess(CurrentUser user) {
        return user.hasPermission(Permissions.ALL_TABLES_ACCESS);
    }

    public boolean canAccessTable(CurrentUser user, RestaurantTable table) {
        if (user.isTableBound()) {
            return table.getId().equals(user.tableId());
        }
        if (hasAllTablesAccess(user)) {
            return true;
        }
        UserAccount waiter = table.getWaiter();
        return waiter != null && waiter.getId().equals(user.id());
    }

    public void checkTableAccess(CurrentUser user, RestaurantTable table) {
        if (!canAccessTable(user, table)) {
            throw new ForbiddenOperationException(user.isTableBound()
                    ? "Yalnızca kendi masanızın kayıtlarına erişebilirsiniz."
                    : "Masa " + table.getTableNumber() + " size atanmamış; bu masanın kayıtlarına erişemezsiniz.");
        }
    }
}
