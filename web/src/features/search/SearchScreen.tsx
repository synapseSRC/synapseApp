import React, { useState } from 'react';
import { Search, UserCheck, Hash, TrendingUp } from 'lucide-react';
import { Input } from '@/components/ui/Input';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';

const SEARCH_SUGGESTIONS = [
  { name: 'Elena Rostova', handle: '@elena_r', avatar: 'https://i.pravatar.cc/150?u=elena', bio: 'AI Engineer & Tech Writer' },
  { name: 'Alex Rivera', handle: '@arivera', avatar: 'https://i.pravatar.cc/150?u=alex', bio: 'Product Designer at Synapse' },
  { name: 'David Chen', handle: '@dchen_dev', avatar: 'https://i.pravatar.cc/150?u=david', bio: 'Kotlin & Web Enthusiast' },
];

const POPULAR_HASHTAGS = [
  { tag: '#SynapseWeb', count: '14.2k posts' },
  { tag: '#React18', count: '9.8k posts' },
  { tag: '#TypeScript', count: '22.1k posts' },
  { tag: '#TailwindCSS', count: '8.4k posts' },
];

export const SearchScreen: React.FC = () => {
  const [query, setQuery] = useState('');
  const [activeTab, setActiveTab] = useState<'top' | 'people' | 'tags'>('top');

  const filteredPeople = SEARCH_SUGGESTIONS.filter(
    (p) =>
      p.name.toLowerCase().includes(query.toLowerCase()) ||
      p.handle.toLowerCase().includes(query.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Search Bar Input */}
      <div className="sticky top-0 z-20 bg-background/95 backdrop-blur border-b p-3">
        <Input
          placeholder="Search creators, topics, or hashtags..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          icon={<Search className="w-4 h-4 text-primary-500" />}
          className="rounded-full bg-muted/60"
        />

        {/* Search Category Filter Tabs */}
        <div className="flex space-x-2 mt-3">
          {(['top', 'people', 'tags'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-4 py-1.5 rounded-full text-xs font-semibold cursor-pointer capitalize transition-all ${
                activeTab === tab
                  ? 'bg-primary-500 text-white shadow-sm'
                  : 'text-muted-foreground hover:bg-muted'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>
      </div>

      {/* People Results */}
      {(activeTab === 'top' || activeTab === 'people') && (
        <div className="space-y-3">
          <h3 className="text-sm font-bold text-muted-foreground uppercase tracking-wider px-1">
            People
          </h3>
          <div className="divide-y border rounded-2xl bg-card overflow-hidden">
            {filteredPeople.map((person, idx) => (
              <div key={idx} className="flex items-center justify-between p-3.5 hover:bg-muted/40 transition-colors">
                <div className="flex items-center space-x-3 min-w-0">
                  <Avatar size="md">
                    <AvatarImage src={person.avatar} alt={person.name} />
                    <AvatarFallback>{person.name.slice(0, 2)}</AvatarFallback>
                  </Avatar>
                  <div className="min-w-0">
                    <p className="text-sm font-semibold truncate">{person.name}</p>
                    <p className="text-xs text-muted-foreground truncate">{person.handle}</p>
                    <p className="text-xs text-muted-foreground/80 truncate mt-0.5">{person.bio}</p>
                  </div>
                </div>
                <Button size="sm" variant="outline" className="rounded-full gap-1 h-8 text-xs px-3 shrink-0">
                  <UserCheck className="w-3.5 h-3.5" />
                  <span>Follow</span>
                </Button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Hashtags & Topics */}
      {(activeTab === 'top' || activeTab === 'tags') && (
        <div className="space-y-3">
          <h3 className="text-sm font-bold text-muted-foreground uppercase tracking-wider px-1 flex items-center space-x-1">
            <TrendingUp className="w-4 h-4 text-primary-500" />
            <span>Trending Hashtags</span>
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {POPULAR_HASHTAGS.map((item, idx) => (
              <div
                key={idx}
                className="flex items-center space-x-3 p-3.5 border rounded-2xl bg-card hover:border-primary-500/50 cursor-pointer transition-colors"
              >
                <div className="p-2.5 rounded-xl bg-primary-500/10 text-primary-500">
                  <Hash className="w-5 h-5" />
                </div>
                <div>
                  <p className="text-sm font-semibold">{item.tag}</p>
                  <p className="text-xs text-muted-foreground">{item.count}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
