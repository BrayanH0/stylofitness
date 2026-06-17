import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Navbar } from '../../components/navbar/navbar';

@Component({
  selector: 'app-ubicacion',
  standalone: true,
  imports: [CommonModule, Navbar],
  templateUrl: './ubicacion.html',
  styleUrls: ['./ubicacion.css']
})
export class Ubicacion { }

