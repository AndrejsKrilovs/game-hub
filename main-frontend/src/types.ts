export interface Game {
  id: string;
  title: string;
  icon: string;
  active: boolean;
  startEndpoint?: string;
  targetUrl?: string;
}