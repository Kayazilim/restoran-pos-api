package com.restoran.pos.service;

import com.restoran.pos.dto.table.QrCredentialsResponse;
import com.restoran.pos.dto.table.TableCreateRequest;
import com.restoran.pos.dto.table.TableCreatedResponse;
import com.restoran.pos.dto.table.TablePatchRequest;
import com.restoran.pos.dto.table.TableResponse;
import com.restoran.pos.dto.table.TableUpdateRequest;
import com.restoran.pos.security.CurrentUser;

import java.util.List;

public interface TableService {

    List<TableResponse> findAll(CurrentUser user);

    TableResponse findById(Long id, CurrentUser user);

    TableCreatedResponse create(TableCreateRequest request);

    TableResponse update(Long id, TableUpdateRequest request);

    TableResponse patch(Long id, TablePatchRequest request);

    void delete(Long id);

    QrCredentialsResponse regenerateQrCode(Long id);
}
