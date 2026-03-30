import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, Inject, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { Location } from '../../../core/models/Location.model';
import { BusinessLineCatalogItem, CoreCatalogService } from '../../../core/services/core-catalog.service';

export interface LocationDialogData {
  location?: Location;
  businessLines?: BusinessLineCatalogItem[];
}

@Component({
  selector: 'app-location-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  template: `
    <h2 id="loc-dialog-title" mat-dialog-title>{{ data.location ? 'Edit Location' : 'Add Location' }}</h2>

    <form id="loc-dialog-form" [formGroup]="locationForm" (ngSubmit)="submit()" mat-dialog-content class="dialog-form">
      <mat-form-field appearance="outline">
        <mat-label>Location Name</mat-label>
        <input id="loc-input-location-name" matInput formControlName="locationName" />
        @if (locationForm.controls['locationName'].invalid && locationForm.controls['locationName'].touched) {
          <mat-error>Location Name is required.</mat-error>
        }
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>ZIP Code</mat-label>
        <input id="loc-input-zip-code" matInput formControlName="zipCode" (blur)="validateZipCode()" />
        @if (locationForm.controls['zipCode'].invalid && locationForm.controls['zipCode'].touched) {
          <mat-error>ZIP Code must have 5 digits.</mat-error>
        }
        @if (zipValidationMessage()) {
          <mat-hint>{{ zipValidationMessage() }}</mat-hint>
        }
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Address</mat-label>
        <input id="loc-input-address" matInput formControlName="address" />
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Giro</mat-label>
        <mat-select id="loc-select-giro" formControlName="giro">
          @for (businessLine of businessLineCatalog(); track businessLine._id) {
            <mat-option [value]="businessLine.code">{{ businessLine.code }} - {{ businessLine.name }}</mat-option>
          }
        </mat-select>
        @if (locationForm.controls['giro'].invalid && locationForm.controls['giro'].touched) {
          <mat-error>Giro is required.</mat-error>
        }
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Fire Key</mat-label>
        <input id="loc-input-fire-key" matInput formControlName="fireKey" />
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Building Value</mat-label>
        <input id="loc-input-building-value" matInput type="number" formControlName="buildingValue" />
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Contents Value</mat-label>
        <input id="loc-input-contents-value" matInput type="number" formControlName="contentsValue" />
      </mat-form-field>
    </form>

    <div mat-dialog-actions align="end">
      <button id="loc-btn-cancel" mat-button type="button" (click)="close()">Cancel</button>
      <button id="loc-btn-save" mat-flat-button color="primary" type="submit" [disabled]="locationForm.invalid" (click)="submit()">
        Save
      </button>
    </div>
  `,
  styles: [
    `
      .dialog-form {
        display: grid;
        grid-template-columns: 1fr;
        gap: 1rem;
        min-width: min(540px, 80vw);
        padding-top: 0.5rem;
      }
    `
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LocationDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<LocationDialogComponent>);
  private readonly coreCatalogService = inject(CoreCatalogService);
  private readonly destroyRef = inject(DestroyRef);

  readonly locationForm: FormGroup;
  readonly businessLineCatalog = signal<BusinessLineCatalogItem[]>([]);
  readonly zipValidationMessage = signal('');

  constructor(@Inject(MAT_DIALOG_DATA) readonly data: LocationDialogData) {
    this.businessLineCatalog.set(this.data.businessLines ?? []);

    this.locationForm = this.fb.nonNullable.group({
      locationName: [this.data.location?.locationName ?? '', [Validators.required, Validators.minLength(2)]],
      zipCode: [this.data.location?.zipCode ?? '', [Validators.required, Validators.pattern(/^\d{5}$/)]],
      address: [this.data.location?.address ?? this.data.location?.direccion ?? ''],
      giro: [this.data.location?.giro ?? '', [Validators.required]],
      fireKey: [this.data.location?.fireKey ?? this.data.location?.claveIncendio ?? ''],
      buildingValue: [this.data.location?.buildingValue ?? 0, [Validators.min(0)]],
      contentsValue: [this.data.location?.contentsValue ?? 0, [Validators.min(0)]]
    });

    if (this.businessLineCatalog().length === 0) {
      this.coreCatalogService
        .getBusinessLines()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((catalog) => this.businessLineCatalog.set(catalog));
    }
  }

  validateZipCode(): void {
    const zipCode = String(this.locationForm.controls['zipCode'].value ?? '').trim();

    if (!/^\d{5}$/.test(zipCode)) {
      this.zipValidationMessage.set('');
      return;
    }

    this.coreCatalogService
      .validateZipCode(zipCode)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (!response.is_valid) {
            this.zipValidationMessage.set('Código postal no válido en catálogo.');
            return;
          }

          const city = response.city ?? 'N/A';
          const state = response.state ?? 'N/A';
          const risk = response.risk_zone ?? 'N/A';
          this.zipValidationMessage.set(`CP válido: ${city}, ${state} (Riesgo: ${risk})`);
        },
        error: () => {
          this.zipValidationMessage.set('No se pudo validar el código postal.');
        }
      });
  }

  close(): void {
    this.dialogRef.close();
  }

  submit(): void {
    if (this.locationForm.invalid) {
      this.locationForm.markAllAsTouched();
      return;
    }

    this.dialogRef.close({
      ...this.data.location,
      ...this.locationForm.getRawValue()
    } as Location);
  }
}
