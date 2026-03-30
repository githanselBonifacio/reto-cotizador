import { CurrencyPipe } from '@angular/common';
import {
  FormArray,
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  signal
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatStepperModule } from '@angular/material/stepper';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { MatTableModule } from '@angular/material/table';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize, switchMap } from 'rxjs/operators';

import { Location } from '../../core/models/Location.model';
import { EstadoCotizacion, QuoteSummary } from '../../core/models/Cotizacion.model';
import {
  LocationsSummaryResponse,
  LocationsLayoutResponse,
  QuoteService,
  UpdateCoverageOptionsRequest,
  UpdateLocationsLayoutRequest
} from '../../core/services/quote.service';
import {
  AgentCatalogItem,
  BusinessLineCatalogItem,
  CoreCatalogService,
  GuaranteeCatalogItem
} from '../../core/services/core-catalog.service';
import { LocationDialogComponent } from './components/location-dialog.component';

type LayoutValueType = 'string' | 'number' | 'boolean';

@Component({
  selector: 'app-quote-creation',
  imports: [
    ReactiveFormsModule,
    CurrencyPipe,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSnackBarModule,
    MatSelectModule,
    MatStepperModule,
    MatTableModule
  ],
  templateUrl: './quote-creation.component.html',
  styleUrl: './quote-creation.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuoteCreationComponent {
  private readonly fb = inject(FormBuilder);
  private readonly quoteService = inject(QuoteService);
  private readonly coreCatalogService = inject(CoreCatalogService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isLoading = signal(false);
  readonly isCalculating = signal(false);
  readonly locations = signal<Location[]>([]);
  readonly locationsSummary = signal<LocationsSummaryResponse | null>(null);
  readonly quoteSummary = signal<QuoteSummary | null>(null);
  readonly selectedStepIndex = signal(0);
  readonly showRfcGuide = signal(false);
  readonly showTechnicalGuide = signal(false);
  readonly isGeneratingPdf = signal(false);
  readonly termsAcceptedLocked = signal(false);
  readonly agentCatalog = signal<AgentCatalogItem[]>([]);
  readonly businessLineCatalog = signal<BusinessLineCatalogItem[]>([]);
  readonly guaranteesCatalog = signal<GuaranteeCatalogItem[]>([]);
  readonly selectedGuarantees = signal<string[]>([]);

  readonly currentQuote = toSignal(this.quoteService.currentQuote$, { initialValue: null });
  readonly isCalculated = computed(() => {
    const status = this.currentQuote()?.status;
    return status === EstadoCotizacion.CALCULATED || status === EstadoCotizacion.CALCULADO;
  });

  readonly generalInfoForm = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.minLength(2)]],
    rfc: ['', [Validators.required, Validators.pattern(/^([A-Z\u00d1&]{3,4})\d{6}([A-Z\d]{3})$/)]],
    codigoAgente: ['', [Validators.required, Validators.minLength(3)]]
  });

  readonly technicalInfoForm = this.fb.group({
    opcionesCoberturaCsv: this.fb.nonNullable.control('', [Validators.required]),
    layoutEntries: this.fb.array([this.createLayoutEntry()])
  });

  readonly termsForm = this.fb.nonNullable.group({
    acceptedTerms: [false, [Validators.requiredTrue]]
  });

  readonly displayedColumns = [
    'indice',
    'locationName',
    'zipCode',
    'giro',
    'buildingValue',
    'contentsValue',
    'status',
    'actions'
  ];

  readonly validLocationsCount = computed(() =>
    this.locations().filter((location) => this.isLocationValid(location)).length
  );

  readonly totalPrimaNeta = computed(() => this.quoteSummary()?.totalPrimaNeta ?? this.locationsSummary()?.totalPrimaNeta ?? 0);
  readonly totalPrimaComercial = computed(() => this.quoteSummary()?.totalPrimaComercial ?? this.locationsSummary()?.totalPrimaComercial ?? 0);

  private loadedFolio: string | null = null;
  private readonly termsStoragePrefix = 'accepted-terms';

  constructor() {
    this.route.url.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((segments) => {
      const lastSegment = segments.at(-1)?.path ?? 'general-info';
      const nextStepIndex = this.resolveStepIndex(lastSegment);
      this.selectedStepIndex.set(nextStepIndex);
      this.triggerStepFiveAutoCalculation(nextStepIndex);
    });

    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const folio = params.get('folio');

      if (folio && folio !== this.loadedFolio) {
        this.loadExistingFolio(folio);
        return;
      }

      if (!folio && !this.loadedFolio) {
        this.initializeWorkflow();
      }
    });

    this.quoteService.refreshRequested$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        const currentQuote = this.currentQuote();

        if (!currentQuote?.folio) {
          return;
        }

        this.reloadFromBackend(currentQuote.folio);
      });

    this.loadCatalogData();

    this.termsForm.controls.acceptedTerms.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((accepted) => {
        const folio = this.currentQuote()?.folio ?? this.loadedFolio;

        if (!folio) {
          return;
        }

        if (this.termsAcceptedLocked() && !accepted) {
          this.termsForm.patchValue({ acceptedTerms: true }, { emitEvent: false });
          return;
        }

        if (accepted) {
          this.persistTermsAcceptance(folio);
        }
      });

    effect(() => {
      if (this.isCalculated()) {
        this.generalInfoForm.disable({ emitEvent: false });
        this.technicalInfoForm.disable({ emitEvent: false });
        this.termsForm.disable({ emitEvent: false });
        return;
      }

      this.generalInfoForm.enable({ emitEvent: false });
      this.technicalInfoForm.enable({ emitEvent: false });
      this.termsForm.enable({ emitEvent: false });
    });
  }

  onStepChange(event: StepperSelectionEvent): void {
    this.selectedStepIndex.set(event.selectedIndex);
    this.triggerStepFiveAutoCalculation(event.selectedIndex);

    const currentQuote = this.currentQuote();

    if (!currentQuote?.folio) {
      return;
    }

    const routeSegment = this.routeSegmentForStep(event.selectedIndex);

    if (!routeSegment) {
      return;
    }

    this.router.navigate(['/quotes', currentQuote.folio, routeSegment]);
  }

  toggleRfcGuide(): void {
    this.showRfcGuide.update((show) => !show);
  }

  toggleTechnicalGuide(): void {
    this.showTechnicalGuide.update((show) => !show);
  }

  onGuaranteesSelectionChange(values: string[]): void {
    const normalizedValues = values.map((item) => item.trim()).filter((item) => item.length > 0);
    this.selectedGuarantees.set(normalizedValues);
    this.technicalInfoForm.patchValue({
      opcionesCoberturaCsv: normalizedValues.join(', ')
    });
  }

  get layoutEntries(): FormArray {
    return this.technicalInfoForm.get('layoutEntries') as FormArray;
  }

  saveGeneralInfo(): void {
    if (this.blockIfCalculated()) {
      return;
    }

    if (this.generalInfoForm.invalid) {
      this.generalInfoForm.markAllAsTouched();
      return;
    }

    const currentQuote = this.currentQuote();

    if (!currentQuote?.folio) {
      this.snackBar.open('Error: Folio no disponible.', 'Cerrar', { duration: 2000 });
      return;
    }

    const formValue = this.generalInfoForm.getRawValue();

    this.quoteService
      .updateGeneralInfo(currentQuote.folio, {
        version: currentQuote.version,
        datosAsegurado: {
          nombre: formValue.nombre,
          rfc: formValue.rfc.toUpperCase()
        },
        codigoAgente: formValue.codigoAgente
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.snackBar.open('Datos generales actualizados.', 'Cerrar', {
            duration: 2500,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
          this.loadGeneralInfo(currentQuote.folio);
        },
        error: (err) => {
          console.error('Error saving general info:', err);
          this.snackBar.open('Error al guardar datos generales.', 'Cerrar', {
            duration: 2500,
            panelClass: ['error-snackbar']
          });
        }
      });
  }

  saveTechnicalInfo(): void {
    if (this.blockIfCalculated()) {
      return;
    }

    if (this.technicalInfoForm.invalid) {
      this.technicalInfoForm.markAllAsTouched();
      return;
    }

    if (this.hasLayoutEntriesWithoutKey()) {
      this.snackBar.open('Completa la clave en las filas técnicas que tengan valor.', 'Cerrar', {
        duration: 2800,
        horizontalPosition: 'right',
        verticalPosition: 'top'
      });
      return;
    }

    const currentQuote = this.currentQuote();

    if (!currentQuote?.folio) {
      this.snackBar.open('Error: Folio no disponible.', 'Cerrar', { duration: 2000 });
      return;
    }

    const formValue = this.technicalInfoForm.getRawValue();
    const coverageOptions = formValue.opcionesCoberturaCsv
      .split(',')
      .map((item: string) => item.trim())
      .filter((item: string) => item.length > 0);

    const layoutConfig = this.buildLayoutConfig();

    this.quoteService
      .getQuoteState(currentQuote.folio)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .pipe(
        switchMap((latestState) => {
          const coverageRequest: UpdateCoverageOptionsRequest = {
            version: latestState.version,
            opcionesCobertura: coverageOptions
          };

          return this.quoteService.updateCoverageOptions(currentQuote.folio, coverageRequest);
        }),
        switchMap((coverageResponse) => {
          const layoutRequest: UpdateLocationsLayoutRequest = {
            version: coverageResponse.version,
            configuracionLayout: layoutConfig
          };

          return this.quoteService.updateLocationsLayout(currentQuote.folio, layoutRequest);
        })
      )
      .subscribe({
        next: () => {
          this.snackBar.open('Informacion tecnica guardada.', 'Cerrar', {
            duration: 2500,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
          this.loadTechnicalInfo(currentQuote.folio);
        },
        error: (err) => {
          console.error('Error saving technical info:', err);
          this.snackBar.open('Error al guardar informacion tecnica.', 'Cerrar', {
            duration: 2500,
            panelClass: ['error-snackbar']
          });
        }
      });
  }

  addLayoutEntry(): void {
    if (this.blockIfCalculated()) {
      return;
    }

    this.layoutEntries.push(this.createLayoutEntry());
  }

  removeLayoutEntry(index: number): void {
    if (this.blockIfCalculated()) {
      return;
    }

    if (this.layoutEntries.length === 1) {
      this.layoutEntries.at(0).reset({
        key: '',
        value: '',
        valueType: 'string'
      });
      return;
    }

    this.layoutEntries.removeAt(index);
  }

  openAddLocationDialog(): void {
    if (this.blockIfCalculated()) {
      return;
    }

    const dialogRef = this.dialog.open(LocationDialogComponent, {
      data: {
        businessLines: this.businessLineCatalog()
      },
      width: '620px'
    });

    dialogRef
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (location?: Location) => {
          if (!location) {
            return;
          }

          const currentQuote = this.currentQuote();

          if (!currentQuote?.folio) {
            this.snackBar.open('Error: Folio no disponible.', 'Cerrar', { duration: 2000 });
            return;
          }

          const updatedLocations = [...this.locations(), location];
          const request = this.quoteService.toUpdateLocationsRequest(updatedLocations, currentQuote.version);

          this.quoteService
            .updateLocations(currentQuote.folio, request)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: () => {
                this.snackBar.open('Ubicacion agregada.', 'Cerrar', { duration: 2000 });
                this.loadLocations(currentQuote.folio);
              },
              error: (err) => {
                console.error('Error adding location:', err);
                this.snackBar.open('Error al agregar ubicacion.', 'Cerrar', {
                  duration: 2500,
                  panelClass: ['error-snackbar']
                });
              }
            });
        }
      });
  }

  openEditLocationDialog(location: Location): void {
    if (this.blockIfCalculated()) {
      return;
    }

    const dialogRef = this.dialog.open(LocationDialogComponent, {
      data: {
        location,
        businessLines: this.businessLineCatalog()
      },
      width: '620px'
    });

    dialogRef
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedLocation?: Location) => {
          if (!updatedLocation) {
            return;
          }

          const currentQuote = this.currentQuote();

          if (!currentQuote?.folio) {
            this.snackBar.open('Error: Folio no disponible.', 'Cerrar', { duration: 2000 });
            return;
          }

          const index =
            typeof location.indice === 'number' ? location.indice : this.locations().findIndex((item) => item === location);

          if (index < 0) {
            this.snackBar.open('Error: No se encontro la ubicacion.', 'Cerrar', { duration: 2000 });
            return;
          }

          const request = this.quoteService.toPatchLocationByIndexRequest(updatedLocation, currentQuote.version);

          this.quoteService
            .patchLocationByIndex(currentQuote.folio, index, request)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: () => {
                this.snackBar.open('Ubicacion actualizada.', 'Cerrar', { duration: 2000 });
                this.loadLocations(currentQuote.folio);
              },
              error: (err) => {
                console.error('Error editing location:', err);
                this.snackBar.open('Error al actualizar ubicacion.', 'Cerrar', {
                  duration: 2500,
                  panelClass: ['error-snackbar']
                });
              }
            });
        }
      });
  }

  deleteLocation(locationToDelete: Location): void {
    if (this.blockIfCalculated()) {
      return;
    }

    const currentQuote = this.currentQuote();

    if (!currentQuote?.folio) {
      this.snackBar.open('Error: Folio no disponible.', 'Cerrar', { duration: 2000 });
      return;
    }

    const updatedLocations = this.locations().filter((location) => location !== locationToDelete);
    const request = this.quoteService.toUpdateLocationsRequest(updatedLocations, currentQuote.version);

    this.quoteService
      .updateLocations(currentQuote.folio, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.snackBar.open('Ubicacion eliminada.', 'Cerrar', { duration: 2000 });
          this.loadLocations(currentQuote.folio);
        },
        error: (err) => {
          console.error('Error deleting location:', err);
          this.snackBar.open('Error al eliminar ubicacion.', 'Cerrar', {
            duration: 2500,
            panelClass: ['error-snackbar']
          });
        }
      });
  }

  calculate(): void {
    if (this.isCalculated()) {
      return;
    }

    if (this.termsForm.invalid) {
      this.termsForm.markAllAsTouched();
      this.snackBar.open('Debes aceptar terminos y condiciones para calcular.', 'Cerrar', {
        duration: 3200,
        horizontalPosition: 'right',
        verticalPosition: 'top'
      });
      return;
    }

    const currentQuote = this.currentQuote();

    if (!currentQuote?.folio) {
      this.snackBar.open('Error: Folio no disponible.', 'Cerrar', { duration: 2000 });
      return;
    }

    this.isCalculating.set(true);

    this.quoteService
      .getQuoteState(currentQuote.folio)
      .pipe(
        switchMap((latestState) =>
          this.quoteService.calculateQuote(currentQuote.folio, {
            version: latestState.version,
            parametros_calculo: {
              factorRiesgo: 1,
              factorComercial: 1
            }
          })
        ),
        switchMap(() => forkJoin({
          state: this.quoteService.getQuoteState(currentQuote.folio),
          summary: this.quoteService.getSummary(currentQuote.folio),
          locationsSummary: this.quoteService.getLocationsSummary(currentQuote.folio).pipe(catchError(() => of(null)))
        })),
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isCalculating.set(false))
      )
      .subscribe({
        next: ({ summary, locationsSummary }) => {
          this.snackBar.open('Cotizacion calculada exitosamente.', 'Cerrar', {
            duration: 2500,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
          this.quoteSummary.set(summary);
          this.locationsSummary.set(locationsSummary);
        },
        error: (err) => {
          console.error('Error calculating quote:', err);
          this.snackBar.open('Error al calcular cotizacion.', 'Cerrar', {
            duration: 2500,
            panelClass: ['error-snackbar']
          });
        }
      });
  }

  locationStatus(location: Location): 'VALID' | 'INCOMPLETE' {
    if (location.estadoValidacion === 'VALID' || location.estadoValidacion === 'VALIDADA') {
      return 'VALID';
    }

    return this.isLocationValid(location) ? 'VALID' : 'INCOMPLETE';
  }

  private initializeWorkflow(): void {
    this.isLoading.set(true);

    this.quoteService
      .initializeQuote()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (quote) => {
          this.loadedFolio = quote.numeroFolio;
          this.router.navigate(['/quotes', quote.numeroFolio, 'general-info']);
          this.reloadFromBackend(quote.numeroFolio);
        },
        error: (err) => {
          console.error('Error initializing quote:', err);
          this.snackBar.open('Error creando nueva cotizacion.', 'Cerrar', {
            duration: 3000,
            panelClass: ['error-snackbar']
          });
        }
      });
  }

  private loadExistingFolio(folio: string): void {
    this.isLoading.set(true);

    this.quoteService
      .getQuoteState(folio)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe({
        next: (state) => {
          this.loadedFolio = state.folio;
          this.reloadFromBackend(state.folio);
        },
        error: (err) => {
          console.error('Error loading folio:', err);
          this.snackBar.open('Error cargando folio.', 'Cerrar', {
            duration: 3000,
            panelClass: ['error-snackbar']
          });
        }
      });
  }

  private reloadFromBackend(folio: string): void {
    this.restoreTermsAcceptance(folio);
    this.loadGeneralInfo(folio);
    this.loadLocations(folio);
    this.loadTechnicalInfo(folio);
    this.loadLocationsSummary(folio);
    this.loadSummary(folio);
  }

  private triggerStepFiveAutoCalculation(stepIndex: number): void {
    if (stepIndex !== 4 || this.isCalculating() || this.isCalculated()) {
      return;
    }

    if (!this.termsForm.valid) {
      return;
    }

    this.calculate();
  }

  private restoreTermsAcceptance(folio: string): void {
    const accepted = sessionStorage.getItem(this.termsStorageKey(folio)) === 'true';
    this.termsAcceptedLocked.set(accepted);
    this.termsForm.patchValue({ acceptedTerms: accepted }, { emitEvent: false });
  }

  private persistTermsAcceptance(folio: string): void {
    sessionStorage.setItem(this.termsStorageKey(folio), 'true');
    this.termsAcceptedLocked.set(true);
    this.termsForm.patchValue({ acceptedTerms: true }, { emitEvent: false });
  }

  private termsStorageKey(folio: string): string {
    return `${this.termsStoragePrefix}:${folio}`;
  }

  private loadGeneralInfo(folio: string): void {
    console.log('Loading general info for folio:', folio);
    this.quoteService
      .getGeneralInfo(folio)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError((err) => {
          console.error('Error loading general info:', err);
          return of(null);
        })
      )
      .subscribe((generalInfo) => {
        if (!generalInfo) {
          console.warn('No general info received');
          return;
        }

        console.log('General info loaded:', generalInfo);
        this.generalInfoForm.patchValue({
          nombre: generalInfo.datosAsegurado?.nombre ?? '',
          rfc: generalInfo.datosAsegurado?.rfc ?? '',
          codigoAgente: generalInfo.codigoAgente ?? ''
        });
      });
  }

  private loadLocations(folio: string): void {
    console.log('Loading locations for folio:', folio);
    this.quoteService
      .getLocations(folio)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError((err) => {
          console.error('Error loading locations:', err);
          return of({ locations: [], version: this.currentQuote()?.version ?? 0 });
        })
      )
      .subscribe((response) => {
        console.log('Locations loaded:', response.locations);
        this.locations.set(response.locations);
      });
  }

  private loadTechnicalInfo(folio: string): void {
    forkJoin({
      coverage: this.quoteService.getCoverageOptions(folio).pipe(catchError(() => of(null))),
      layout: this.quoteService.getLocationsLayout(folio).pipe(catchError(() => of(null)))
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ coverage, layout }) => {
          const coverageValue = coverage?.opcionesCobertura?.join(', ') ?? '';
          this.selectedGuarantees.set(coverage?.opcionesCobertura ?? []);

          this.technicalInfoForm.patchValue({
            opcionesCoberturaCsv: coverageValue
          });
          this.setLayoutEntries(layout);
        },
        error: (err) => {
          console.error('Error loading technical info:', err);
        }
      });
  }

  private loadLocationsSummary(folio: string): void {
    console.log('Loading locations summary for folio:', folio);
    this.quoteService
      .getLocationsSummary(folio)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError((err) => {
          console.error('Error loading locations summary:', err);
          return of(null);
        })
      )
      .subscribe((summary) => {
        console.log('Locations summary loaded:', summary);
        this.locationsSummary.set(summary);
      });
  }

  private loadSummary(folio: string): void {
    this.quoteService
      .getSummary(folio)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError((err) => {
          console.error('Error loading quote summary:', err);
          return of(null);
        })
      )
      .subscribe((summary) => {
        this.quoteSummary.set(summary);
      });
  }

  private loadCatalogData(): void {
    forkJoin({
      agents: this.coreCatalogService.getAgents().pipe(catchError(() => of([]))),
      businessLines: this.coreCatalogService.getBusinessLines().pipe(catchError(() => of([]))),
      guarantees: this.coreCatalogService.getGuarantees().pipe(catchError(() => of([])))
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ agents, businessLines, guarantees }) => {
        this.agentCatalog.set(agents.filter((agent) => agent.status?.toUpperCase() !== 'INACTIVE'));
        this.businessLineCatalog.set(businessLines.filter((line) => line.status?.toUpperCase() !== 'INACTIVE'));
        this.guaranteesCatalog.set(guarantees);
      });
  }

  private loadState(folio: string): void {
    console.log('Loading state for folio:', folio);
    this.quoteService
      .getQuoteState(folio)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError((err) => {
          console.error('Error loading state:', err);
          return of(null);
        })
      )
      .subscribe((state) => {
        if (state) {
          console.log('State loaded:', state);
        }
      });
  }

  async generateQuotePdf(): Promise<void> {
    const currentQuote = this.currentQuote();

    if (!currentQuote?.folio || !this.isCalculated()) {
      this.snackBar.open('Solo puedes generar el PDF cuando la cotización esté en estado CALCULATED.', 'Cerrar', {
        duration: 3000,
        horizontalPosition: 'right',
        verticalPosition: 'top'
      });
      return;
    }

    this.isGeneratingPdf.set(true);

    try {
      const { jsPDF } = await import('jspdf');
      const pdf = new jsPDF({ unit: 'mm', format: 'a4' });
      const generalInfo = this.generalInfoForm.getRawValue();
      const technicalInfo = this.technicalInfoForm.getRawValue();
      const layoutConfig = this.buildLayoutConfig();
      const currencyFormatter = new Intl.NumberFormat('es-CO', {
        style: 'currency',
        currency: 'COP',
        maximumFractionDigits: 0
      });

      const pageWidth = pdf.internal.pageSize.getWidth();
      const pageHeight = pdf.internal.pageSize.getHeight();
      const margin = 14;
      const contentWidth = pageWidth - margin * 2;
      let cursorY = 18;

      const ensureSpace = (requiredHeight: number) => {
        if (cursorY + requiredHeight <= pageHeight - 16) {
          return;
        }

        pdf.addPage();
        cursorY = 18;
      };

      const drawSectionTitle = (title: string) => {
        ensureSpace(12);
        pdf.setFont('helvetica', 'bold');
        pdf.setFontSize(12);
        pdf.setTextColor(66, 36, 125);
        pdf.text(title, margin, cursorY);
        pdf.setDrawColor(214, 205, 235);
        pdf.line(margin, cursorY + 2, pageWidth - margin, cursorY + 2);
        cursorY += 8;
      };

      const drawLabelValue = (label: string, value: string, x: number, y: number) => {
        pdf.setFont('helvetica', 'bold');
        pdf.setFontSize(9);
        pdf.setTextColor(107, 94, 149);
        pdf.text(label.toUpperCase(), x, y);
        pdf.setFont('helvetica', 'bold');
        pdf.setFontSize(12);
        pdf.setTextColor(23, 20, 53);
        pdf.text(value || 'N/A', x, y + 7);
      };

      const drawInfoRows = (rows: Array<[string, string]>) => {
        rows.forEach(([label, value]) => {
          const labelText = `${label}:`;
          const labelWidth = pdf.getTextWidth(labelText);
          const minValueX = margin + 32;
          const maxValueX = pageWidth - margin - 65;
          const valueX = Math.min(maxValueX, Math.max(minValueX, margin + labelWidth + 6));
          const valueWidth = Math.max(40, pageWidth - margin - valueX);
          const wrapped = pdf.splitTextToSize(value || 'N/A', valueWidth);
          const rowHeight = Math.max(8, wrapped.length * 5 + 3);
          ensureSpace(rowHeight + 2);
          pdf.setFont('helvetica', 'bold');
          pdf.setFontSize(10);
          pdf.setTextColor(66, 36, 125);
          pdf.text(labelText, margin, cursorY);
          pdf.setFont('helvetica', 'normal');
          pdf.setTextColor(45, 45, 45);
          pdf.text(wrapped, valueX, cursorY);
          cursorY += rowHeight;
        });
      };

      pdf.setFillColor(84, 54, 145);
      pdf.roundedRect(margin, cursorY - 8, contentWidth, 28, 4, 4, 'F');
      pdf.setTextColor(255, 255, 255);
      pdf.setFont('helvetica', 'bold');
      pdf.setFontSize(20);
      pdf.text('Cotización de Daños', margin + 6, cursorY + 3);
      pdf.setFont('helvetica', 'normal');
      pdf.setFontSize(10);
      pdf.text('Resumen final de la cotización generada', margin + 6, cursorY + 10);
      cursorY += 30;

      pdf.setFillColor(248, 245, 255);
      pdf.setDrawColor(221, 214, 243);
      pdf.roundedRect(margin, cursorY, contentWidth, 20, 3, 3, 'FD');
      drawLabelValue('Folio', currentQuote.folio, margin + 5, cursorY + 6);
      drawLabelValue('Estado', String(currentQuote.status), margin + 68, cursorY + 6);
      drawLabelValue('Fecha', new Date().toLocaleDateString('es-CO'), margin + 122, cursorY + 6);
      cursorY += 28;

      drawSectionTitle('Datos generales');
      drawInfoRows([
        ['Nombre', generalInfo.nombre || 'N/A'],
        ['RFC', generalInfo.rfc || 'N/A'],
        ['Código agente', generalInfo.codigoAgente || 'N/A']
      ]);

      cursorY += 2;
      drawSectionTitle('Coberturas');
      const coverages = (technicalInfo.opcionesCoberturaCsv || 'N/A')
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean);
      const coverageText = coverages.length > 0 ? coverages.join('   •   ') : 'N/A';
      drawInfoRows([['Coberturas seleccionadas', coverageText]]);

      cursorY += 2;
      drawSectionTitle('Configuración técnica');
      const layoutRows = Object.entries(layoutConfig);
      if (layoutRows.length === 0) {
        drawInfoRows([['Configuración', 'Sin datos técnicos adicionales']]);
      } else {
        drawInfoRows(layoutRows.map(([key, value]) => [key, String(value)]));
      }

      cursorY += 2;
      drawSectionTitle('Ubicaciones');
      const locations = this.locations();
      if (locations.length === 0) {
        drawInfoRows([['Ubicaciones', 'No hay ubicaciones registradas']]);
      } else {
        ensureSpace(12);
        const tableX = margin;
        const tableWidths = [12, 68, 28, 34, 30];
        const headers = ['#', 'Ubicación', 'CP', 'Giro', 'Valor'];
        let currentX = tableX;
        pdf.setFillColor(94, 63, 160);
        pdf.setTextColor(255, 255, 255);
        pdf.setFont('helvetica', 'bold');
        pdf.setFontSize(9);
        headers.forEach((header, index) => {
          pdf.rect(currentX, cursorY, tableWidths[index], 8, 'F');
          pdf.text(header, currentX + 2, cursorY + 5.3);
          currentX += tableWidths[index];
        });
        cursorY += 8;

        pdf.setFont('helvetica', 'normal');
        pdf.setTextColor(35, 35, 35);
        locations.forEach((location, index) => {
          ensureSpace(9);
          const rowValues = [
            String(index + 1),
            String(location.locationName ?? location.nombreUbicacion ?? 'Ubicación'),
            String(location.zipCode ?? location.codigoPostal ?? 'N/A'),
            String(location.giro ?? 'N/A'),
            currencyFormatter.format(location.buildingValue ?? location.insuredAmount ?? 0)
          ];
          currentX = tableX;
          rowValues.forEach((value, valueIndex) => {
            pdf.setDrawColor(225, 225, 225);
            pdf.rect(currentX, cursorY, tableWidths[valueIndex], 8);
            const clipped = pdf.splitTextToSize(value, tableWidths[valueIndex] - 3)[0] || '';
            pdf.text(clipped, currentX + 2, cursorY + 5.3);
            currentX += tableWidths[valueIndex];
          });
          cursorY += 8;
        });
      }

      cursorY += 4;
      ensureSpace(28);
      pdf.setFillColor(243, 238, 255);
      pdf.roundedRect(margin, cursorY, contentWidth, 24, 3, 3, 'F');
      pdf.setFont('helvetica', 'bold');
      pdf.setFontSize(12);
      pdf.setTextColor(66, 36, 125);
      pdf.text('Resumen económico', margin + 5, cursorY + 7);
      pdf.setFontSize(11);
      pdf.setTextColor(23, 20, 53);
      pdf.text(`Prima neta total: ${currencyFormatter.format(this.totalPrimaNeta())}`, margin + 5, cursorY + 14);
      pdf.text(`Prima comercial total: ${currencyFormatter.format(this.totalPrimaComercial())}`, margin + 5, cursorY + 20);

      pdf.setFont('helvetica', 'normal');
      pdf.setFontSize(8);
      pdf.setTextColor(120, 120, 120);
      pdf.text('Documento generado desde Plataforma Daños Web', margin, pageHeight - 8);

      pdf.save(`cotizacion-${currentQuote.folio}.pdf`);
      this.snackBar.open('PDF generado correctamente.', 'Cerrar', {
        duration: 2500,
        horizontalPosition: 'right',
        verticalPosition: 'top'
      });
    } catch (error) {
      console.error('Error generating PDF:', error);
      this.snackBar.open('No fue posible generar el PDF.', 'Cerrar', {
        duration: 3000,
        panelClass: ['error-snackbar']
      });
    } finally {
      this.isGeneratingPdf.set(false);
    }
  }

  private resolveStepIndex(lastSegment: string): number {
    if (lastSegment === 'general-info') {
      return 0;
    }

    if (lastSegment === 'locations') {
      return 1;
    }

    if (lastSegment === 'technical-info') {
      return 2;
    }

    if (lastSegment === 'terms-and-conditions') {
      return 3;
    }

    if (lastSegment === 'calculation-summary') {
      return 4;
    }

    if (lastSegment === 'confirmation') {
      return 5;
    }

    return 0;
  }

  private routeSegmentForStep(index: number): string | null {
    if (index === 0) {
      return 'general-info';
    }

    if (index === 1) {
      return 'locations';
    }

    if (index === 2) {
      return 'technical-info';
    }

    if (index === 3) {
      return 'terms-and-conditions';
    }

    if (index === 4) {
      return 'calculation-summary';
    }

    if (index === 5) {
      return 'confirmation';
    }

    return null;
  }

  private isLocationValid(location: Location): boolean {
    const locationName = location.locationName ?? location.nombreUbicacion ?? '';
    const zipCode = location.zipCode ?? location.codigoPostal ?? '';

    return Boolean(
      locationName.trim().length > 1 && zipCode.length >= 5 && location.giro && location.giro.trim().length > 0
    );
  }

  private createLayoutEntry(key = '', value = '', valueType: LayoutValueType = 'string') {
    return this.fb.nonNullable.group({
      key: [key],
      value: [value],
      valueType: [valueType, [Validators.required]]
    });
  }

  hasLayoutEntryKeyError(index: number): boolean {
    const entryControl = this.layoutEntries.at(index);
    const key = String(entryControl.get('key')?.value ?? '').trim();
    const value = String(entryControl.get('value')?.value ?? '').trim();

    return !key && value.length > 0;
  }

  private hasLayoutEntriesWithoutKey(): boolean {
    return this.layoutEntries.controls.some((entryControl) => {
      const key = String(entryControl.get('key')?.value ?? '').trim();
      const value = String(entryControl.get('value')?.value ?? '').trim();

      return !key && value.length > 0;
    });
  }

  private blockIfCalculated(): boolean {
    if (!this.isCalculated()) {
      return false;
    }

    this.snackBar.open('La cotización ya está en estado CALCULATED y no permite modificaciones.', 'Cerrar', {
      duration: 3000,
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
    return true;
  }

  private setLayoutEntries(layout: LocationsLayoutResponse | null): void {
    this.layoutEntries.clear();

    const entries = layout?.configuracionLayout ? Object.entries(layout.configuracionLayout) : [];

    if (entries.length === 0) {
      this.layoutEntries.push(this.createLayoutEntry());
      return;
    }

    for (const [key, rawValue] of entries) {
      this.layoutEntries.push(this.createLayoutEntry(key, this.formatLayoutValue(rawValue), this.resolveLayoutValueType(rawValue)));
    }
  }

  private buildLayoutConfig(): Record<string, unknown> {
    const config: Record<string, unknown> = {};

    for (const entryControl of this.layoutEntries.controls) {
      const entryValue = entryControl.getRawValue() as {
        key: string;
        value: string;
        valueType: LayoutValueType;
      };

      const key = entryValue.key.trim();

      if (!key) {
        continue;
      }

      config[key] = this.parseLayoutValue(entryValue.value, entryValue.valueType);
    }

    return config;
  }

  private resolveLayoutValueType(value: unknown): LayoutValueType {
    if (typeof value === 'number') {
      return 'number';
    }

    if (typeof value === 'boolean') {
      return 'boolean';
    }

    return 'string';
  }

  private formatLayoutValue(value: unknown): string {
    if (value === null || value === undefined) {
      return '';
    }

    return String(value);
  }

  private parseLayoutValue(value: string, valueType: LayoutValueType): string | number | boolean {
    if (valueType === 'number') {
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : 0;
    }

    if (valueType === 'boolean') {
      return ['true', '1', 'si', 'sí', 'yes'].includes(value.trim().toLowerCase());
    }

    return value;
  }
}
