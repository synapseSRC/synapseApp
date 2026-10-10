import React from 'react';
import { Check, CheckCheck, Clock, AlertCircle } from 'lucide-react';
import { ChatMessage } from '@/types';
import { formatDate } from '@/lib/utils';

interface MessageBubbleProps {
  message: ChatMessage;
  isOwn: boolean;
}

export const MessageBubble: React.FC<MessageBubbleProps> = ({ message, isOwn }) => {
  const renderStatusIcon = () => {
    switch (message.status) {
      case 'SENDING':
        return <Clock className="w-3 h-3 text-muted-foreground animate-spin" />;
      case 'SENT':
        return <Check className="w-3 h-3 text-muted-foreground" />;
      case 'DELIVERED':
        return <CheckCheck className="w-3 h-3 text-muted-foreground" />;
      case 'READ':
        return <CheckCheck className="w-3 h-3 text-primary-500 font-bold" />;
      case 'FAILED':
        return <AlertCircle className="w-3 h-3 text-destructive" />;
      default:
        return null;
    }
  };

  return (
    <div className={`flex flex-col my-1 max-w-[80%] ${isOwn ? 'ml-auto items-end' : 'mr-auto items-start'}`}>
      <div
        className={`px-4 py-2.5 rounded-2xl text-sm leading-relaxed shadow-xs ${
          isOwn
            ? 'bg-primary-500 text-white rounded-br-none'
            : 'bg-muted text-foreground rounded-bl-none border border-border/50'
        }`}
      >
        {message.mediaUrl && (
          <div className="mb-2 rounded-lg overflow-hidden">
            {message.mediaType === 'image' && (
              <img src={message.mediaUrl} alt="Attached media" className="max-w-full max-h-60 object-cover" />
            )}
            {message.mediaType === 'video' && (
              <video src={message.mediaUrl} controls className="max-w-full max-h-60 rounded-lg" />
            )}
          </div>
        )}
        <p className="whitespace-pre-wrap break-words">{message.content}</p>
      </div>

      <div className={`flex items-center space-x-1 mt-1 text-[10px] text-muted-foreground ${isOwn ? 'justify-end' : 'justify-start'}`}>
        <span>{formatDate(message.createdAt)}</span>
        {isOwn && <span>{renderStatusIcon()}</span>}
      </div>
    </div>
  );
};
