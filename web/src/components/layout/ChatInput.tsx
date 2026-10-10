import React, { useState } from 'react';
import { Send, Image, Mic, Smile } from 'lucide-react';
import { Button } from '@/components/ui/Button';

interface ChatInputProps {
  onSendMessage: (text: string, mediaUrl?: string) => void;
  disabled?: boolean;
  placeholder?: string;
}

export const ChatInput: React.FC<ChatInputProps> = ({
  onSendMessage,
  disabled = false,
  placeholder = 'Type a message...',
}) => {
  const [text, setText] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!text.trim() || disabled) return;
    onSendMessage(text.trim());
    setText('');
  };

  return (
    <form onSubmit={handleSubmit} className="flex items-center space-x-2 p-3 border-t bg-card/80 backdrop-blur">
      <Button size="icon" variant="ghost" type="button" className="text-muted-foreground hover:text-foreground">
        <Image className="w-5 h-5" />
      </Button>
      <Button size="icon" variant="ghost" type="button" className="text-muted-foreground hover:text-foreground">
        <Smile className="w-5 h-5" />
      </Button>

      <input
        type="text"
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder={placeholder}
        disabled={disabled}
        className="flex-1 bg-muted/60 text-foreground text-sm rounded-full px-4 py-2 focus:outline-none focus:ring-2 focus:ring-primary-500 transition-all placeholder:text-muted-foreground"
      />

      {text.trim() ? (
        <Button size="icon" type="submit" disabled={disabled} className="rounded-full bg-primary-500 hover:bg-primary-600 text-white shrink-0">
          <Send className="w-4 h-4" />
        </Button>
      ) : (
        <Button size="icon" variant="ghost" type="button" className="text-muted-foreground hover:text-foreground">
          <Mic className="w-5 h-5" />
        </Button>
      )}
    </form>
  );
};
