import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { EvenementPublic } from '../../core/models/evenement.model';
import { EvenementService } from '../../core/services/evenement.service';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.scss'
})
export class LandingPageComponent implements OnInit {
  currentYear = new Date().getFullYear();
  events: EvenementPublic[] = [];
  eventsLoading = true;
  eventsError = '';

  constructor(private readonly evenementService: EvenementService) {}

  ngOnInit(): void {
    this.evenementService.getEvenementsPublicsAVenir().subscribe({
      next: events => {
        this.events = (events ?? []).filter(event => event && Number.isFinite(event.id));
        this.eventsLoading = false;
      },
      error: error => {
        console.error('Impossible de charger les événements publics de la page d’accueil', error);
        this.eventsError = 'Les événements à venir ne peuvent pas être chargés pour le moment.';
        this.eventsLoading = false;
      }
    });
  }

  typeEvenementLabel(type: string | null | undefined): string {
    switch (type) {
      case 'CONFERENCE': return 'Conférence';
      case 'SEMINAIRE': return 'Séminaire';
      case 'WORKSHOP': return 'Workshop';
      case 'SOUTENANCE_THESE': return 'Soutenance';
      default: return 'Événement scientifique';
    }
  }

  stats = [
    { value: '4', label: 'Équipes permanentes', detail: 'ISDIAC · SIMA · ILIAS · SDTIC' },
    { value: '230+', label: 'Publications indexées', detail: 'Scopus & Web of Science' },
    { value: '42', label: 'Thèses en cours', detail: '2021 – 2024' },
    { value: '28', label: 'Thèses soutenues', detail: 'Doctorats délivrés' },
    { value: '8', label: 'Projets de recherche', detail: 'Nationaux & internationaux' }
  ];

  researchAxes = [
    {
      icon: 'brain',
      title: 'IA & Optimisation',
      description: 'XAI, apprentissage profond, IA générative et méthodes d\'optimisation avancées.',
      tags: ['XAI', 'Deep Learning', 'GenAI']
    },
    {
      icon: 'database',
      title: 'Systèmes & Données',
      description: 'Cloud computing, architectures Lakehouse, vision par ordinateur et Big Data.',
      tags: ['Cloud', 'Lakehouse', 'Vision']
    },
    {
      icon: 'shield',
      title: 'IoT & Cybersécurité',
      description: 'Edge computing, systèmes intelligents embarqués et sécurité des réseaux.',
      tags: ['Edge', 'IoT', 'Security']
    },
    {
      icon: 'languages',
      title: 'NLP & Multimodal',
      description: 'Traitement automatique des langues française et arabe, analyse multimodale.',
      tags: ['NLP', 'FR/AR', 'Multimodal']
    },
    {
      icon: 'heart',
      title: 'Santé & Smart Cities',
      description: 'e-Santé, transport urbain intelligent et services numériques pour la cité.',
      tags: ['e-Santé', 'Transport', 'Urban']
    },
    {
      icon: 'chart',
      title: 'Modélisation avancée',
      description: 'Modélisation stochastique, systèmes multi-agents et simulation complexe.',
      tags: ['Stochastique', 'Multi-agents', 'Simulation']
    }
  ];

  teamMembers = [
    { name: 'Pr. Faouzia Benabbou', role: 'Directrice du laboratoire', highlight: true },
    { name: 'Pr. Abdessamad Belangour', role: 'Directeur adjoint', highlight: true },
    { name: 'Pr. Nawal Sael', role: 'Cheffe d\'équipe ISDIAC', highlight: false },
    { name: 'Pr. Mohammed Ait Daoud', role: 'Chef d\'équipe SIMA', highlight: false },
    { name: 'Pr. Sara Ouahabi', role: 'Cheffe d\'équipe SDTIC', highlight: false },
    { name: 'Pr. Abdelaziz Ettaoufik', role: 'Chef d\'équipe ILIAS', highlight: false }
  ];

}
