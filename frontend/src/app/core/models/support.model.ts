export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING_RESPONSE' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TicketCategory = 'BILLING' | 'TECHNICAL' | 'VPS' | 'DOMAIN' | 'KUBERNETES' | 'ACCOUNT' | 'OTHER';

export interface Ticket {
  id: number;
  userId: number;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  category: TicketCategory | null;
  assignedAgentId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface TicketComment {
  id: number;
  authorId: number;
  isAgent: boolean;
  content: string;
  createdAt: string;
}

export interface CreateTicketRequest {
  title: string;
  description: string;
  priority?: TicketPriority;
  category?: TicketCategory;
}

export interface CreateTicketForClientRequest extends CreateTicketRequest {
  userId: number;
}

export interface TicketAttachment {
  id: number;
  ticketId: number;
  commentId: number | null;
  filename: string;
  contentType: string;
  size: number;
  uploadedByUserId: number;
  createdAt: string;
}
