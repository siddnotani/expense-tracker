package com.siddnotani.expensetracker.bff;

import jakarta.validation.Valid;
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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static com.siddnotani.expensetracker.bff.BffDtos.Budget;
import static com.siddnotani.expensetracker.bff.BffDtos.Category;

@RestController
@RequestMapping("/api")
@Validated
public class BudgetProxyController {
    private final BudgetClient client;
    private final BudgetCompositionService composition;

    public BudgetProxyController(BudgetClient client, BudgetCompositionService composition) {
        this.client = client;
        this.composition = composition;
    }

    @GetMapping("/categories")
    public Mono<List<Category>> categories(
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return client.categories(includeArchived);
    }

    @PostMapping("/categories/defaults")
    public Mono<List<Category>> defaults() {
        return client.defaults();
    }

    @PostMapping("/categories")
    public Mono<ResponseEntity<Category>> createCategory(
            @Valid @RequestBody BffDtos.CreateCategoryRequest body) {
        return client.createCategory(body).map(category -> ResponseEntity.status(201).body(category));
    }

    @PatchMapping("/categories/{id}")
    public Mono<Category> updateCategory(
            @PathVariable UUID id, @Valid @RequestBody BffDtos.UpdateCategoryRequest body) {
        return client.updateCategory(id, body);
    }

    @DeleteMapping("/categories/{id}")
    public Mono<ResponseEntity<Void>> archiveCategory(@PathVariable UUID id) {
        return client.archiveCategory(id).thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping("/budgets")
    public Mono<List<Budget>> budgets(@RequestParam String period) {
        return client.budgets(period);
    }

    @PutMapping("/budgets")
    public Mono<Budget> upsertBudget(
            @Valid @RequestBody BffDtos.UpsertBudgetRequest body) {
        return client.upsertBudget(body);
    }

    @DeleteMapping("/budgets/{id}")
    public Mono<ResponseEntity<Void>> deleteBudget(@PathVariable UUID id) {
        return client.deleteBudget(id).thenReturn(ResponseEntity.noContent().build());
    }

    @PostMapping("/budgets/copy")
    public Mono<ResponseEntity<Void>> copyBudgets(
            @Valid @RequestBody BffDtos.CopyBudgetsRequest body) {
        return client.copyBudgets(body).thenReturn(ResponseEntity.ok().build());
    }

    @GetMapping("/budgets/progress")
    public Mono<BffDtos.BudgetProgress> progress(@RequestParam(required = false) String period) {
        return composition.progress(period);
    }

    @GetMapping("/dashboard")
    public Mono<BffDtos.Dashboard> dashboard(@RequestParam(required = false) String period) {
        return composition.dashboard(period);
    }
}
