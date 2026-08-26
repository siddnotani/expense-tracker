package com.siddnotani.expensetracker.budget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.siddnotani.expensetracker.budget.BudgetDtos.BudgetResponse;
import static com.siddnotani.expensetracker.budget.BudgetDtos.CategoryResponse;

@RestController
@RequestMapping("/internal")
@Validated
public class BudgetController {
    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    @PostMapping("/categories/defaults")
    public List<CategoryResponse> defaults() {
        return service.createDefaults().stream().map(CategoryResponse::from).toList();
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories(
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return service.categories(includeArchived).stream().map(CategoryResponse::from).toList();
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody BudgetDtos.CreateCategoryRequest request) {
        Category category = service.createCategory(request);
        return ResponseEntity.created(URI.create("/internal/categories/" + category.getId()))
                .body(CategoryResponse.from(category));
    }

    @PatchMapping("/categories/{id}")
    public CategoryResponse update(
            @PathVariable UUID id, @Valid @RequestBody BudgetDtos.UpdateCategoryRequest request) {
        return CategoryResponse.from(service.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        service.archiveCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/budgets")
    public List<BudgetResponse> budgets(@RequestParam @NotBlank String period) {
        return service.budgets(period).stream().map(BudgetResponse::from).toList();
    }

    @PutMapping("/budgets")
    public BudgetResponse upsert(@Valid @RequestBody BudgetDtos.UpsertBudgetRequest request) {
        return BudgetResponse.from(service.upsertBudget(request));
    }

    @DeleteMapping("/budgets/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable UUID id) {
        service.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/budgets/copy")
    public ResponseEntity<Void> copy(@Valid @RequestBody BudgetDtos.CopyBudgetsRequest request) {
        service.copyBudgets(request.fromPeriod(), request.toPeriod());
        return ResponseEntity.ok().build();
    }
}
