package com.restoran.pos.controller;

import com.restoran.pos.dto.knowledge.KnowledgeDocumentCreateRequest;
import com.restoran.pos.dto.knowledge.KnowledgeDocumentPatchRequest;
import com.restoran.pos.dto.knowledge.KnowledgeDocumentResponse;
import com.restoran.pos.dto.knowledge.KnowledgeDocumentUpdateRequest;
import com.restoran.pos.entity.DocumentType;
import com.restoran.pos.security.Permissions;
import com.restoran.pos.service.KnowledgeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/knowledge-documents")
@Tag(name = "Asistan Belgeleri", description = "Asistanın tek bilgi kaynağı olan menü ve alerjen belgeleri (yalnızca YÖNETİCİ)")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;

    public KnowledgeDocumentController(KnowledgeDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.KNOWLEDGE_READ + "')")
    @Operation(summary = "Belgeleri listele")
    public List<KnowledgeDocumentResponse> findAll(
            @Parameter(description = "Belge türü filtresi", example = "ALLERGEN") @RequestParam(required = false) DocumentType type) {
        return documentService.findAll(type);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.KNOWLEDGE_READ + "')")
    @Operation(summary = "Kimliğe göre belge getir")
    public KnowledgeDocumentResponse findById(@PathVariable Long id) {
        return documentService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.KNOWLEDGE_WRITE + "')")
    @Operation(summary = "Belge yükle", description = "Belge eklendiğinde asistanın vektör indeksi güncellenir.")
    @ApiResponse(responseCode = "201", description = "Belge oluşturuldu",
            content = @Content(schema = @Schema(implementation = KnowledgeDocumentResponse.class)))
    public ResponseEntity<KnowledgeDocumentResponse> create(@Valid @RequestBody KnowledgeDocumentCreateRequest request) {
        KnowledgeDocumentResponse created = documentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.KNOWLEDGE_WRITE + "')")
    @Operation(summary = "Belgeyi tam güncelle (PUT)")
    public KnowledgeDocumentResponse update(@PathVariable Long id, @Valid @RequestBody KnowledgeDocumentUpdateRequest request) {
        return documentService.update(id, request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.KNOWLEDGE_WRITE + "')")
    @Operation(summary = "Belgeyi kısmi güncelle (PATCH)")
    public KnowledgeDocumentResponse patch(@PathVariable Long id, @Valid @RequestBody KnowledgeDocumentPatchRequest request) {
        return documentService.patch(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.KNOWLEDGE_DELETE + "')")
    @Operation(summary = "Belgeyi sil")
    @ApiResponse(responseCode = "204", description = "Belge silindi")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
