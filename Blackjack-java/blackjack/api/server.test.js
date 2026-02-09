const request = require('supertest');
const app = require('./server'); // Importa a app, mas não inicia o servidor
const { firebaseRequest } = require('./firebase');

// Faz o mock de todo o módulo 'firebase' para que `firebaseRequest` possa ser espiado.
jest.mock('./firebase', () => ({
  firebaseRequest: jest.fn(),
}));

describe('Users API', () => {

  // Limpa os mocks antes de cada teste para garantir isolamento
  beforeEach(() => {
    firebaseRequest.mockClear();
  });

  describe('GET /users', () => {
    it('deve retornar uma lista de utilizadores quando eles existem', async () => {
      const mockUsers = {
        user1: { id: 'user1', displayName: 'Test User 1', wallet: 100 },
        user2: { id: 'user2', displayName: 'Test User 2', wallet: 200 },
      };
      // Configura o mock para devolver os nossos utilizadores falsos
      firebaseRequest.mockResolvedValue(mockUsers);

      const response = await request(app).get('/users');

      expect(response.statusCode).toBe(200);
      expect(response.body).toEqual(mockUsers);
      expect(firebaseRequest).toHaveBeenCalledWith('GET', 'users.json', null);
      expect(firebaseRequest).toHaveBeenCalledTimes(1);
    });

    it('deve retornar um objeto vazio se não houver utilizadores', async () => {
      firebaseRequest.mockResolvedValue(null);

      const response = await request(app).get('/users');

      expect(response.statusCode).toBe(200);
      expect(response.body).toEqual({});
    });
  });

  describe('POST /users', () => {
    it('deve criar um novo utilizador e retornar 201 se ele não existir', async () => {
      const newUser = { id: 'newUser1', displayName: 'Newbie', wallet: 500 };

      // 1. Mock para a verificação de existência (retorna null)
      firebaseRequest.mockResolvedValueOnce(null);
      // 2. Mock para a operação de escrita (não precisa retornar nada)
      firebaseRequest.mockResolvedValueOnce(null);

      const response = await request(app)
        .post('/users')
        .send(newUser);

      expect(response.statusCode).toBe(201);
      expect(response.body).toEqual(newUser);
      // Verifica se as duas chamadas ao Firebase foram feitas como esperado
      expect(firebaseRequest).toHaveBeenCalledTimes(2);
      expect(firebaseRequest).toHaveBeenCalledWith('GET', `users/${newUser.id}.json`, null);
      expect(firebaseRequest).toHaveBeenCalledWith('PUT', `users/${newUser.id}.json`, newUser);
    });

    it('deve retornar 409 (Conflict) se o utilizador já existir', async () => {
      const existingUser = { id: 'user1', displayName: 'Old User', wallet: 1000 };
      firebaseRequest.mockResolvedValue(existingUser);

      const response = await request(app)
        .post('/users')
        .send({ id: 'user1', displayName: 'Trying To Recreate' });

      expect(response.statusCode).toBe(409);
      expect(response.body.error).toBe('User already exists');
      expect(firebaseRequest).toHaveBeenCalledTimes(1);
    });

    it('deve retornar 400 (Bad Request) se o ID estiver em falta', async () => {
        const response = await request(app)
            .post('/users')
            .send({ displayName: 'No ID' });
        expect(response.statusCode).toBe(400);
    });
  });

  describe('GET /users/:id', () => {
    it('deve retornar um utilizador específico se ele for encontrado', async () => {
      const mockUser = { id: 'user1', displayName: 'Found User', wallet: 50 };
      firebaseRequest.mockResolvedValue(mockUser);

      const response = await request(app).get('/users/user1');

      expect(response.statusCode).toBe(200);
      expect(response.body).toEqual(mockUser);
      expect(firebaseRequest).toHaveBeenCalledWith('GET', 'users/user1.json', null);
    });

    it('deve retornar 404 se o utilizador não for encontrado', async () => {
      firebaseRequest.mockResolvedValue(null);

      const response = await request(app).get('/users/nonexistent');

      expect(response.statusCode).toBe(404);
      expect(response.body.error).toBe('User not found');
    });
  });

  describe('PUT /users/:id/wallet', () => {
    it('deve atualizar a carteira e retornar o utilizador atualizado', async () => {
      const existingUser = { id: 'user1', displayName: 'Wallet User', wallet: 100 };
      const newWalletData = { wallet: 500 };

      // Mock para o GET inicial que verifica a existência
      firebaseRequest.mockResolvedValueOnce(existingUser);
      // Mock para o PATCH que atualiza os dados
      firebaseRequest.mockResolvedValueOnce(null);

      const response = await request(app)
        .put('/users/user1/wallet')
        .send(newWalletData);

      expect(response.statusCode).toBe(200);
      expect(response.body.id).toBe('user1');
      expect(response.body.wallet).toBe(500);
      expect(firebaseRequest).toHaveBeenCalledTimes(2);
      expect(firebaseRequest).toHaveBeenCalledWith('PATCH', 'users/user1.json', newWalletData);
    });

    it('deve retornar 404 se o utilizador a ser atualizado não for encontrado', async () => {
      firebaseRequest.mockResolvedValue(null);

      const response = await request(app)
        .put('/users/nonexistent/wallet')
        .send({ wallet: 999 });

      expect(response.statusCode).toBe(404);
    });
  });

  describe('DELETE /users/:id', () => {
    it('deve retornar 204 No Content após apagar com sucesso', async () => {
      // Mock para o GET que verifica a existência
      firebaseRequest.mockResolvedValueOnce({ id: 'userToDelete' });
      // Mock para o DELETE
      firebaseRequest.mockResolvedValueOnce(null);

      const response = await request(app).delete('/users/userToDelete');

      expect(response.statusCode).toBe(204);
      expect(firebaseRequest).toHaveBeenCalledTimes(2);
      expect(firebaseRequest).toHaveBeenCalledWith('DELETE', 'users/userToDelete.json', null);
    });

    it('deve retornar 404 se o utilizador a ser apagado não for encontrado', async () => {
      firebaseRequest.mockResolvedValue(null);
      const response = await request(app).delete('/users/nonexistent');
      expect(response.statusCode).toBe(404);
    });
  });
});