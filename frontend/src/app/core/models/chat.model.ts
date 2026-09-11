export interface MessageRequest {
  userId: number;
  conversationId?: number;
  message: string;
}

export interface MessageResponseAction {
  type: string;
  data: unknown;
}

export interface MessageResponse {
  message: string;
  action: MessageResponseAction | null;
  conversationId: number;
}

export interface HistoryMessageDto {
  role: 'user' | 'assistant';
  content: string;
  action: string | null;
  timestamp: string;
}

export interface Conversation {
  id: number;
  userId: number;
  title: string | null;
  createdAt: string;
  updatedAt: string;
}

// Shape returned by GET /api/chat/conversations/{id}/messages (raw Message entity).
export interface ConversationMessage {
  id: number;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  detectedAction: string | null;
  actionResult: string | null;
  createdAt: string;
}

// Client-side chat bubble shape — not a backend contract.
export interface ChatMessage {
  id: number;
  role: 'user' | 'assistant' | 'error';
  content: string;
  timestamp: string;
}