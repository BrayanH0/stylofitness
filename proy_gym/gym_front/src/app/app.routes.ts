import { Routes } from '@angular/router';
import { Principal } from './pages/principal/principal';
import { AuthGuard } from './guards/auth.guard';
import { AdminGuard } from './guards/admin.guard';
import { PersonalGuard } from './guards/personal.guard';

export const routes: Routes = [
    { path: '', component: Principal },
    {
        path: 'interfaz-usuario',
        loadComponent: () => import('./pages/interfaz-usuario/interfaz-usuario.component').then(m => m.InterfazUsuarioComponent),
        canActivate: [AuthGuard]
    },
    {
        path: 'personal',
        loadComponent: () => import('./pages/interfaz-personal/interfaz-personal.component').then(m => m.InterfazPersonalComponent),
        canActivate: [AuthGuard, PersonalGuard]
    },
    {
        path: 'admin',
        loadComponent: () => import('./pages/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent),
        canActivate: [AuthGuard, AdminGuard]
    },
    {
        path: 'ubicacion',
        loadComponent: () => import('./pages/ubicacion/ubicacion').then(m => m.Ubicacion)
    },
    {
        path: 'registrar',
        loadComponent: () => import('./pages/registrar/registrar').then(m => m.Registrar)
    },
    {
        path: 'pago',
        loadComponent: () => import('./pages/pago/pago.component').then(m => m.PagoComponent)
    },
    {
        path: 'exito',
        loadComponent: () => import('./pages/exito/exito.component').then(m => m.ExitoComponent)
    },
    {
        path: 'cancel',
        loadComponent: () => import('./pages/cancel/cancel.component').then(m => m.CancelComponent)
    },
    { path: '**', redirectTo: '' }
];