import React, { useState } from 'react';
import { Search, ArrowLeft } from 'lucide-react';
import { ChatConversation, ChatMessage } from '@/types';
import { MessageBubble } from '@/components/layout/MessageBubble';
import { ChatInput } from '@/components/layout/ChatInput';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { Input } from '@/components/ui/Input';
import { formatDate } from '@/lib/utils';

const MOCK_CONVERSATIONS: ChatConversation[] = [
  {
    id: 'chat_1',
    participant: {
      id: 'usr_sarah',
      username: 'sarah_tech',
      fullName: 'Sarah Jenkins',
      avatarUrl: 'https://i.pravatar.cc/150?u=sarah',
      followersCount: 3420,
      followingCount: 512,
      postsCount: 128,
      createdAt: new Date().toISOString(),
    },
    lastMessage: {
      id: 'msg_1',
      chatId: 'chat_1',
      senderId: 'usr_sarah',
      content: 'Hey! The new Synapse web interface looks super smooth! ✨',
      status: 'READ',
      createdAt: new Date(Date.now() - 1000 * 60 * 10).toISOString(),
    },
    unreadCount: 0,
    updatedAt: new Date(Date.now() - 1000 * 60 * 10).toISOString(),
  },
  {
    id: 'chat_2',
    participant: {
      id: 'usr_marcus',
      username: 'marcus_code',
      fullName: 'Marcus Vance',
      avatarUrl: 'https://i.pravatar.cc/150?u=marcus',
      followersCount: 890,
      followingCount: 230,
      postsCount: 54,
      createdAt: new Date().toISOString(),
    },
    lastMessage: {
      id: 'msg_2',
      chatId: 'chat_2',
      senderId: 'usr_marcus',
      content: 'Did you get a chance to check out the KMP shared architecture?',
      status: 'DELIVERED',
      createdAt: new Date(Date.now() - 1000 * 60 * 120).toISOString(),
    },
    unreadCount: 2,
    updatedAt: new Date(Date.now() - 1000 * 60 * 120).toISOString(),
  },
];

export const InboxScreen: React.FC = () => {
  const [selectedChat, setSelectedChat] = useState<ChatConversation | null>(MOCK_CONVERSATIONS[0]);
  const [searchQuery, setSearchQuery] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'msg_0',
      chatId: 'chat_1',
      senderId: 'usr_sarah',
      content: 'Hi there! Excited for the Synapse Web launch!',
      status: 'READ',
      createdAt: new Date(Date.now() - 1000 * 60 * 30).toISOString(),
    },
    {
      id: 'msg_1',
      chatId: 'chat_1',
      senderId: 'usr_sarah',
      content: 'Hey! The new Synapse web interface looks super smooth! ✨',
      status: 'READ',
      createdAt: new Date(Date.now() - 1000 * 60 * 10).toISOString(),
    },
  ]);

  const handleSendMessage = (text: string) => {
    if (!selectedChat) return;

    const newMsg: ChatMessage = {
      id: `msg_${Date.now()}`,
      chatId: selectedChat.id,
      senderId: 'usr_current',
      content: text,
      status: 'SENT',
      createdAt: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, newMsg]);
  };

  const filteredConversations = MOCK_CONVERSATIONS.filter(
    (c) =>
      c.participant.fullName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.participant.username.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="flex h-[calc(100vh-5rem)] border rounded-2xl overflow-hidden bg-card shadow-sm">
      {/* Conversations List Panel */}
      <div
        className={`w-full sm:w-80 border-r flex flex-col shrink-0 ${
          selectedChat ? 'hidden sm:flex' : 'flex'
        }`}
      >
        <div className="p-4 border-b space-y-3">
          <h2 className="text-lg font-bold">Messages</h2>
          <Input
            placeholder="Search messages..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            icon={<Search className="w-4 h-4" />}
          />
        </div>

        <div className="flex-1 overflow-y-auto divide-y divide-border/40">
          {filteredConversations.map((chat) => {
            const isSelected = selectedChat?.id === chat.id;
            return (
              <div
                key={chat.id}
                onClick={() => setSelectedChat(chat)}
                className={`flex items-center space-x-3 p-3.5 cursor-pointer transition-colors ${
                  isSelected ? 'bg-primary-500/10 border-l-4 border-primary-500' : 'hover:bg-muted/60'
                }`}
              >
                <Avatar size="md">
                  <AvatarImage src={chat.participant.avatarUrl} alt={chat.participant.fullName} />
                  <AvatarFallback>{chat.participant.fullName.slice(0, 2)}</AvatarFallback>
                </Avatar>

                <div className="flex-1 min-w-0">
                  <div className="flex justify-between items-baseline">
                    <p className="text-sm font-semibold truncate">{chat.participant.fullName}</p>
                    <span className="text-[10px] text-muted-foreground">
                      {chat.lastMessage && formatDate(chat.lastMessage.createdAt)}
                    </span>
                  </div>
                  <p className="text-xs text-muted-foreground truncate mt-0.5">
                    {chat.lastMessage?.content || 'No messages yet'}
                  </p>
                </div>

                {chat.unreadCount > 0 && (
                  <span className="w-5 h-5 rounded-full bg-primary-500 text-white text-[10px] font-bold flex items-center justify-center">
                    {chat.unreadCount}
                  </span>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Chat Thread View Panel */}
      <div className={`flex-1 flex-col bg-background/50 ${selectedChat ? 'flex' : 'hidden sm:flex'}`}>
        {selectedChat ? (
          <>
            {/* Thread Header */}
            <div className="p-3 border-b flex items-center space-x-3 bg-card">
              <button
                onClick={() => setSelectedChat(null)}
                className="sm:hidden p-1.5 rounded-lg hover:bg-muted"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
              <Avatar size="sm">
                <AvatarImage src={selectedChat.participant.avatarUrl} alt={selectedChat.participant.fullName} />
                <AvatarFallback>{selectedChat.participant.fullName.slice(0, 2)}</AvatarFallback>
              </Avatar>
              <div>
                <h3 className="text-sm font-bold leading-none">{selectedChat.participant.fullName}</h3>
                <span className="text-[10px] text-emerald-500 font-semibold">Online</span>
              </div>
            </div>

            {/* Messages Scroll Area */}
            <div className="flex-1 overflow-y-auto p-4 space-y-2">
              {messages.map((msg) => (
                <MessageBubble key={msg.id} message={msg} isOwn={msg.senderId !== selectedChat.participant.id} />
              ))}
            </div>

            {/* Message Composer */}
            <ChatInput onSendMessage={handleSendMessage} />
          </>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center p-6 text-center text-muted-foreground">
            <p className="text-base font-semibold">Select a conversation</p>
            <p className="text-xs mt-1">Choose a contact to start messaging on Synapse Social.</p>
          </div>
        )}
      </div>
    </div>
  );
};
