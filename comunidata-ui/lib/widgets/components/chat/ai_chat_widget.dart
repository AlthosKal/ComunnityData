import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../../../core/controllers/ai_chat_controller.dart';
import 'chat_message_widget.dart';
import 'typing_indicator_widget.dart';

class AIChatWidget extends StatefulWidget {
  const AIChatWidget({super.key});

  @override
  State<AIChatWidget> createState() => _AIChatWidgetState();
}

class _AIChatWidgetState extends State<AIChatWidget> {
  late TextEditingController _messageController;
  late ScrollController _chatScrollController;

  @override
  void initState() {
    super.initState();
    _messageController = TextEditingController();
    _chatScrollController = ScrollController();
  }

  @override
  void dispose() {
    _messageController.dispose();
    _chatScrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<AIChatController>(
      builder: (context, chatController, child) {
        return Container(
          decoration: const BoxDecoration(
            color: Color(0xFF374151),
          ),
          child: Row(
            children: [
              Container(
                width: 320,
                decoration: const BoxDecoration(
                  color: Color(0xFF2D3A4F),
                  border: Border(
                    right: BorderSide(color: Color(0xFF4A5D75), width: 1),
                  ),
                ),
                child: _buildConversationPanel(context, chatController),
              ),
              Expanded(
                child: Column(
                  children: [
                    Expanded(
                      child: _buildChatArea(context, chatController),
                    ),
                    _buildMessageInput(context, chatController),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildConversationPanel(BuildContext context, AIChatController chatController) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.all(20),
          decoration: const BoxDecoration(
            color: Color(0xFF2D3A4F),
            border: Border(bottom: BorderSide(color: Color(0xFF4A5D75), width: 1)),
          ),
          child: Row(
            children: [
              const Icon(Icons.history, color: Color(0xFF8FA3B8), size: 20),
              const SizedBox(width: 8),
              const Expanded(
                child: Text(
                  'Conversaciones',
                  style: TextStyle(
                    fontWeight: FontWeight.w600,
                    color: Colors.white,
                    fontSize: 14,
                  ),
                ),
              ),
              IconButton(
                onPressed: () => _startNewConversation(chatController),
                icon: const Icon(Icons.add, size: 18),
                tooltip: 'Nueva conversación',
                color: const Color(0xFF8FA3B8),
              ),
            ],
          ),
        ),
        Expanded(
          child: Container(
            color: const Color(0xFF2D3A4F),
            child: chatController.isLoading
                ? const Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        CircularProgressIndicator(color: Color(0xFF8FA3B8)),
                        SizedBox(height: 16),
                        Text(
                          'Cargando conversaciones...',
                          style: TextStyle(color: Color(0xFF8FA3B8)),
                        ),
                      ],
                    ),
                  )
                : chatController.conversations.isEmpty
                    ? _buildEmptyConversations()
                    : ListView.builder(
                        padding: const EdgeInsets.all(12),
                        itemCount: chatController.conversations.length,
                        itemBuilder: (context, index) {
                          final conversation = chatController.conversations[index];
                          return _buildConversationTile(
                            context: context,
                            title: conversation['title'],
                            subtitle: conversation['subtitle'],
                            isActive: conversation['isActive'],
                            onTap: () => _loadConversation(chatController, conversation),
                            onDelete: () => _deleteConversation(chatController, conversation['id']),
                          );
                        },
                      ),
          ),
        ),
      ],
    );
  }

  Widget _buildEmptyConversations() {
    return const Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.chat_bubble_outline,
            size: 48,
            color: Color(0xFF8FA3B8),
          ),
          SizedBox(height: 16),
          Text(
            'Sin conversaciones',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w500,
              color: Color(0xFF8FA3B8),
            ),
          ),
          SizedBox(height: 8),
          Text(
            'Inicia tu primera conversación',
            style: TextStyle(
              fontSize: 14,
              color: Color(0xFF6B7280),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildConversationTile({
    required BuildContext context,
    required String title,
    required String subtitle,
    required bool isActive,
    required VoidCallback onTap,
    VoidCallback? onDelete,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 6),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: isActive ? const Color(0xFF4A5D75) : Colors.transparent,
            borderRadius: BorderRadius.circular(8),
            border: isActive ? Border.all(color: const Color(0xFF5A6B82), width: 1) : null,
          ),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: TextStyle(
                        fontWeight: isActive ? FontWeight.w600 : FontWeight.normal,
                        color: isActive ? Colors.white : const Color(0xFF8FA3B8),
                        fontSize: 13,
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      subtitle,
                      style: const TextStyle(
                        fontSize: 11,
                        color: Color(0xFF6B7280),
                      ),
                    ),
                  ],
                ),
              ),
              if (onDelete != null && !isActive) ...[
                const SizedBox(width: 8),
                InkWell(
                  onTap: onDelete,
                  borderRadius: BorderRadius.circular(4),
                  child: const Padding(
                    padding: EdgeInsets.all(4),
                    child: Icon(
                      Icons.delete_outline,
                      size: 16,
                      color: Color(0xFF6B7280),
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildChatArea(BuildContext context, AIChatController chatController) {
    if (chatController.messages.isEmpty && !chatController.isTyping) {
      return _buildEmptyChat(context);
    }

    return Container(
      color: const Color(0xFF374151),
      child: Column(
        children: [
          Expanded(
            child: ListView.builder(
              controller: _chatScrollController,
              padding: const EdgeInsets.all(20),
              itemCount: chatController.messages.length + (chatController.isTyping ? 1 : 0),
              itemBuilder: (context, index) {
                if (index == chatController.messages.length && chatController.isTyping) {
                  return const TypingIndicatorWidget();
                }

                final message = chatController.messages[index];
                return ChatMessageWidget(
                  message: message,
                  onComplete: _scrollToBottom,
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyChat(BuildContext context) {
    return Container(
      color: const Color(0xFF374151),
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: const Color(0xFF4A90E2).withOpacity(0.2),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.chat_bubble_outline,
                size: 56,
                color: Color(0xFF4A90E2),
              ),
            ),
            const SizedBox(height: 24),
            const Text(
              'Bienvenido a ComuniData',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.w600,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              'Generamos propuestas de solución concretas funcionales',
              style: TextStyle(
                fontSize: 14,
                color: Color(0xFF8FA3B8),
                height: 1.5,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMessageInput(BuildContext context, AIChatController chatController) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: const BoxDecoration(
        color: Color(0xFF374151),
        border: Border(top: BorderSide(color: Color(0xFF4A5D75), width: 1)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Focus(
              onKeyEvent: (node, event) {
                if (event is KeyDownEvent && event.logicalKey == LogicalKeyboardKey.enter) {
                  if (HardwareKeyboard.instance.isShiftPressed) {
                    return KeyEventResult.ignored;
                  } else {
                    if (!chatController.isLoading && !chatController.isTyping) {
                      _sendMessage(chatController);
                    }
                    return KeyEventResult.handled;
                  }
                }
                return KeyEventResult.ignored;
              },
              child: TextField(
                controller: _messageController,
                enabled: !chatController.isLoading && !chatController.isTyping,
                style: const TextStyle(color: Colors.white),
                decoration: InputDecoration(
                  hintText: (chatController.isLoading || chatController.isTyping)
                      ? 'Generando respuesta...'
                      : 'Type your message...',
                  hintStyle: const TextStyle(color: Color(0xFF8FA3B8)),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(25),
                    borderSide: const BorderSide(color: Color(0xFF4A5D75)),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(25),
                    borderSide: const BorderSide(color: Color(0xFF4A5D75)),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(25),
                    borderSide: const BorderSide(color: Color(0xFF4A90E2)),
                  ),
                  disabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(25),
                    borderSide: const BorderSide(color: Color(0xFF374151)),
                  ),
                  fillColor: const Color(0xFF4A5D75),
                  filled: true,
                  contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                ),
                maxLines: null,
                textCapitalization: TextCapitalization.sentences,
              ),
            ),
          ),
          const SizedBox(width: 12),
          Container(
            decoration: BoxDecoration(
              color: (chatController.isLoading || chatController.isTyping)
                  ? const Color(0xFFEF4444)
                  : const Color(0xFF4A90E2),
              borderRadius: BorderRadius.circular(25),
            ),
            child: IconButton(
              onPressed: (chatController.isLoading || chatController.isTyping)
                  ? () => _cancelMessage(chatController)
                  : () => _sendMessage(chatController),
              icon: (chatController.isLoading || chatController.isTyping)
                  ? const Icon(Icons.stop, color: Colors.white)
                  : const Icon(Icons.send, color: Colors.white),
              tooltip: (chatController.isLoading || chatController.isTyping)
                  ? 'Cancelar respuesta'
                  : 'Enviar mensaje',
            ),
          ),
        ],
      ),
    );
  }

  void _startNewConversation(AIChatController chatController) {
    chatController.startNewConversation();
    _messageController.clear();
  }

  void _loadConversation(AIChatController chatController, Map<String, dynamic> conversation) {
    chatController.loadConversation(conversation['id']);
  }

  void _deleteConversation(AIChatController chatController, String conversationId) async {
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('Eliminar conversación'),
          content: const Text('¿Estás seguro de que quieres eliminar esta conversación?'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: const Text('Cancelar'),
            ),
            TextButton(
              onPressed: () => Navigator.of(context).pop(true),
              style: TextButton.styleFrom(foregroundColor: Colors.red),
              child: const Text('Eliminar'),
            ),
          ],
        );
      },
    );

    if (confirmed == true) {
      chatController.deleteConversation(conversationId);
    }
  }

  void _sendMessage(AIChatController chatController) {
    final message = _messageController.text.trim();
    if (message.isEmpty) return;

    chatController.sendMessage(message);
    _messageController.clear();
    _scrollToBottom();
  }

  void _cancelMessage(AIChatController chatController) {
    chatController.cancelCurrentResponse();
  }

  void _scrollToBottom() {
    if (_chatScrollController.hasClients) {
      Future.delayed(const Duration(milliseconds: 100), () {
        _chatScrollController.animateTo(
          _chatScrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      });
    }
  }
}
